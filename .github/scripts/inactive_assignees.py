#!/usr/bin/env python3
"""Remind assignees of inactive issues and unassign them after a while.

An assignee is reminded once an issue has had no activity from them for
REMINDER_INTERVAL_DAYS and is unassigned after INACTIVITY_DAYS, provided a
reminder has gone unanswered for a full reminder interval.

The script is executed by .github/workflows/inactive-assignees.yml and only
needs the Python standard library. The decision logic is kept in pure
functions so it can be unit tested with
`python3 -m unittest discover -s .github/scripts`.

Environment variables:
    GITHUB_TOKEN             token with `issues: write`
    GITHUB_REPOSITORY        `owner/repo`
    GITHUB_API_URL           optional, defaults to https://api.github.com
    GITHUB_STEP_SUMMARY      optional, file the run summary is appended to
    INACTIVITY_DAYS          optional, defaults to 30
    REMINDER_INTERVAL_DAYS   optional, defaults to 10
    EXEMPT_LABELS            optional, comma-separated labels to skip
    DRY_RUN                  optional, `true` only logs what would happen
"""
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone

# Scheduled workflows often start a little later than their cron time. Without
# some slack, a reminder posted by a delayed run would look a few minutes short
# of a full interval on the run exactly one interval later.
SCHEDULE_TOLERANCE = timedelta(hours=2)

REMINDER_MARKER_PREFIX = "inactive-assignee-reminder"

DEFAULT_INACTIVITY_DAYS = 30
DEFAULT_REMINDER_INTERVAL_DAYS = 10

# Pull requests linked through the Development sidebar only show up as
# `connected` timeline events, which do not say which pull request was linked
# or whether it is still open, so they are looked up through GraphQL.
LINKED_PULL_REQUESTS_QUERY = """
query ($owner: String!, $repo: String!, $number: Int!, $after: String) {
  repository(owner: $owner, name: $repo) {
    issue(number: $number) {
      closedByPullRequestsReferences(first: 100, after: $after, includeClosedPrs: false) {
        pageInfo {
          hasNextPage
          endCursor
        }
        nodes {
          state
          author {
            login
          }
        }
      }
    }
  }
}
"""


@dataclass(frozen=True)
class Config:
    inactivity_days: int = DEFAULT_INACTIVITY_DAYS
    reminder_interval_days: int = DEFAULT_REMINDER_INTERVAL_DAYS
    exempt_labels: tuple = ()
    dry_run: bool = False


@dataclass
class Decision:
    login: str
    action: str  # "none", "remind" or "unassign"
    idle_days: int = None
    reason: str = None


@dataclass
class IssueResult:
    number: int
    to_unassign: list = field(default_factory=list)
    to_remind: list = field(default_factory=list)


def reminder_marker(login):
    return f"<!-- {REMINDER_MARKER_PREFIX}: {login.lower()} -->"


def parse_positive_integer(name, value, default):
    if value is None or str(value).strip() == "":
        return default
    text = str(value).strip()
    if not re.fullmatch(r"[0-9]+", text) or int(text) < 1:
        raise ValueError(f'{name} must be a positive integer, got "{value}"')
    return int(text)


def parse_config(env):
    inactivity_days = parse_positive_integer(
        "INACTIVITY_DAYS", env.get("INACTIVITY_DAYS"), DEFAULT_INACTIVITY_DAYS)
    reminder_interval_days = parse_positive_integer(
        "REMINDER_INTERVAL_DAYS", env.get("REMINDER_INTERVAL_DAYS"),
        DEFAULT_REMINDER_INTERVAL_DAYS)
    if reminder_interval_days >= inactivity_days:
        raise ValueError(
            f"REMINDER_INTERVAL_DAYS ({reminder_interval_days}) must be smaller than "
            f"INACTIVITY_DAYS ({inactivity_days}), otherwise no reminder would be "
            "sent before an assignee is unassigned")
    labels = tuple(
        label.strip() for label in (env.get("EXEMPT_LABELS") or "").split(",")
        if label.strip())
    return Config(
        inactivity_days=inactivity_days,
        reminder_interval_days=reminder_interval_days,
        exempt_labels=labels,
        dry_run=(env.get("DRY_RUN") or "").strip().lower() == "true",
    )


def same_login(a, b):
    return bool(a) and bool(b) and a.lower() == b.lower()


def login_of(user):
    return (user or {}).get("login")


def is_bot(user):
    login = login_of(user)
    if not login:
        return False
    return user.get("type") == "Bot" or login.endswith("[bot]")


def parse_time(value):
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except (AttributeError, ValueError):
        return None


def is_open_pull_request_by(event, login):
    issue = (event.get("source") or {}).get("issue") or {}
    return (
        bool(issue.get("pull_request"))
        and issue.get("state") == "open"
        and same_login(login_of(issue.get("user")), login)
    )


def is_activity_by(event, login):
    """Return True when the timeline event is progress made by the assignee.

    That is a comment, a reference to this issue from another issue or pull
    request, a commit referencing it, or a pull request linked through the
    Development sidebar. Comments and events from bots, including this
    automation, never count.
    """
    kind = event.get("event")
    if kind == "commented":
        user = event.get("user")
    elif kind in ("cross-referenced", "referenced", "connected"):
        user = event.get("actor")
    else:
        return False
    return not is_bot(user) and same_login(login_of(user), login)


def is_marker_comment_for(event, marker):
    body = event.get("body")
    return (
        event.get("event") == "commented"
        and is_bot(event.get("user"))
        and isinstance(body, str)
        and marker in body
    )


def evaluate_assignee(login, timeline, now, config, open_pull_request_authors=()):
    """Work out what should happen to a single assignee of an issue.

    The decision is based on the issue timeline and the authors of open pull
    requests linked to the issue. The returned action is one of:
      "none"     nothing to do (recently active, already reminded, or has an
                 open pull request for the issue)
      "remind"   post a reminder asking for a progress update
      "unassign" remove the assignee and explain why
    """
    assigned_at = None
    last_activity_at = None
    last_reminder_at = None
    has_open_pull_request = any(
        same_login(author, login) for author in open_pull_request_authors)
    marker = reminder_marker(login)

    for event in timeline:
        if event.get("event") == "cross-referenced" and is_open_pull_request_by(event, login):
            has_open_pull_request = True
        time = parse_time(event.get("created_at"))
        if time is None:
            continue
        if event.get("event") == "assigned" and same_login(
                login_of(event.get("assignee")), login):
            assigned_at = max(assigned_at or time, time)
        elif is_activity_by(event, login):
            last_activity_at = max(last_activity_at or time, time)
        elif is_marker_comment_for(event, marker):
            last_reminder_at = max(last_reminder_at or time, time)

    if assigned_at is None:
        return Decision(login, "none", reason="no assignment event found")
    if has_open_pull_request:
        return Decision(login, "none", reason="has an open pull request for this issue")

    active_since = max(assigned_at, last_activity_at or assigned_at)
    idle_days = (now - active_since) // timedelta(days=1)
    # Only reminders sent after the latest activity matter: any activity from
    # the assignee starts the inactivity period again.
    reminded_at = (
        last_reminder_at
        if last_reminder_at is not None and last_reminder_at > active_since
        else None)
    days_since_reminder = (
        None if reminded_at is None
        else (now - reminded_at + SCHEDULE_TOLERANCE) // timedelta(days=1))

    # An assignee is unassigned only after an unanswered reminder has been on
    # the issue for a full reminder interval, so nobody loses an issue without
    # warning.
    if (idle_days >= config.inactivity_days
            and days_since_reminder is not None
            and days_since_reminder >= config.reminder_interval_days):
        return Decision(login, "unassign", idle_days=idle_days)
    if (idle_days >= config.reminder_interval_days
            and (days_since_reminder is None
                 or days_since_reminder >= config.reminder_interval_days)):
        return Decision(login, "remind", idle_days=idle_days)
    return Decision(login, "none", idle_days=idle_days,
                    reason="recently active or already reminded")


def has_exempt_label(issue, exempt_labels):
    wanted = {label.lower() for label in exempt_labels}
    for label in issue.get("labels") or []:
        name = label if isinstance(label, str) else (label or {}).get("name")
        if name and name.lower() in wanted:
            return True
    return False


def mention_list(logins):
    return ", ".join(f"@{login}" for login in logins)


def build_reminder_comment(logins, config):
    return "\n".join([
        *(reminder_marker(login) for login in logins),
        f"Hi {mention_list(logins)}, this issue is assigned to you but has had no "
        f"activity from you for at least {config.reminder_interval_days} days.",
        "",
        "Could you share a short update on your progress? If you are no longer "
        "working on it, please leave a comment so that someone else can pick it up.",
        "",
        f"Assignees who have no activity on an issue for {config.inactivity_days} days "
        "are unassigned automatically. Commenting here, or opening a pull request "
        "that references this issue, resets the timer.",
    ])


def build_unassigned_comment(logins, config):
    verb = "has" if len(logins) == 1 else "have"
    return "\n".join([
        f"{mention_list(logins)} {verb} been unassigned from this issue because "
        f"there was no activity on it from them for at least "
        f"{config.inactivity_days} days, even after a reminder.",
        "",
        "This issue is now available for other contributors to pick up. If you are "
        "still working on it, leave a comment and a maintainer can assign it back "
        "to you.",
    ])


class GitHubClient:
    """Minimal GitHub REST and GraphQL client built on urllib."""

    def __init__(self, token, api_url="https://api.github.com"):
        self.token = token
        self.api_url = api_url.rstrip("/")

    def _request(self, method, url, payload=None):
        data = None if payload is None else json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(url, data=data, method=method, headers={
            "Authorization": f"Bearer {self.token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Content-Type": "application/json",
        })
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                body = response.read()
                return (json.loads(body) if body else None), response.headers
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", "replace")
            raise RuntimeError(f"{method} {url} failed with {error.code}: {detail}") from error

    def paginate(self, path, params=None):
        query = urllib.parse.urlencode({**(params or {}), "per_page": 100})
        url = f"{self.api_url}{path}?{query}"
        items = []
        while url:
            page, headers = self._request("GET", url)
            items.extend(page)
            match = re.search(r'<([^>]+)>;\s*rel="next"', headers.get("Link") or "")
            url = match.group(1) if match else None
        return items

    def graphql(self, query, variables):
        result, _ = self._request("POST", f"{self.api_url}/graphql",
                                  {"query": query, "variables": variables})
        if result.get("errors"):
            raise RuntimeError(f"GraphQL query failed: {result['errors']}")
        return result["data"]

    def remove_assignees(self, repo, number, logins):
        self._request("DELETE", f"{self.api_url}/repos/{repo}/issues/{number}/assignees",
                      {"assignees": logins})

    def create_comment(self, repo, number, body):
        self._request("POST", f"{self.api_url}/repos/{repo}/issues/{number}/comments",
                      {"body": body})


def fetch_open_linked_pull_request_authors(client, repo, number):
    owner, name = repo.split("/", 1)
    authors = []
    after = None
    while True:
        data = client.graphql(LINKED_PULL_REQUESTS_QUERY,
                              {"owner": owner, "repo": name, "number": number, "after": after})
        issue = (data.get("repository") or {}).get("issue") or {}
        references = issue.get("closedByPullRequestsReferences") or {}
        authors += [
            node["author"]["login"] for node in references.get("nodes") or []
            if node and node.get("state") == "OPEN" and node.get("author")
        ]
        page_info = references.get("pageInfo") or {}
        if not page_info.get("hasNextPage"):
            return authors
        after = page_info["endCursor"]


def process_issue(client, repo, issue, now, config, log):
    number = issue["number"]
    timeline = client.paginate(f"/repos/{repo}/issues/{number}/timeline")
    authors = fetch_open_linked_pull_request_authors(client, repo, number)

    decisions = [
        evaluate_assignee(assignee["login"], timeline, now, config, authors)
        for assignee in issue["assignees"]
    ]
    result = IssueResult(
        number,
        to_unassign=[d.login for d in decisions if d.action == "unassign"],
        to_remind=[d.login for d in decisions if d.action == "remind"],
    )

    for decision in decisions:
        idle = (f" (inactive for {decision.idle_days} days)"
                if decision.idle_days is not None else "")
        reason = f" - {decision.reason}" if decision.reason else ""
        log(f"#{number} @{decision.login}: {decision.action}{idle}{reason}")

    if not config.dry_run:
        if result.to_unassign:
            client.remove_assignees(repo, number, result.to_unassign)
            client.create_comment(
                repo, number, build_unassigned_comment(result.to_unassign, config))
        if result.to_remind:
            client.create_comment(
                repo, number, build_reminder_comment(result.to_remind, config))
    return result


def build_summary(results, config):
    unassign_label = "Would be unassigned" if config.dry_run else "Unassigned"
    remind_label = "Would be reminded" if config.dry_run else "Reminded"
    rows = []
    for result in results:
        rows += [(f"#{result.number}", f"@{login}", unassign_label)
                 for login in result.to_unassign]
        rows += [(f"#{result.number}", f"@{login}", remind_label)
                 for login in result.to_remind]
    heading = ("Inactive assignees (dry run, no changes made)"
               if config.dry_run else "Inactive assignees")
    lines = [f"## {heading}", ""]
    if not rows:
        lines.append("No assignee needed a reminder or had to be unassigned.")
    else:
        lines += ["| Issue | Assignee | Action |", "| --- | --- | --- |"]
        lines += [f"| {issue} | {assignee} | {action} |" for issue, assignee, action in rows]
    return "\n".join(lines) + "\n"


def run(client, repo, env, now=None, log=print, warn=None):
    """Process all open assigned issues and return (results, failures, config)."""
    config = parse_config(env)
    now = now or datetime.now(timezone.utc)
    warn = warn or log
    log(f"Inactivity period: {config.inactivity_days} days, reminder interval: "
        f"{config.reminder_interval_days} days, exempt labels: "
        f"{', '.join(config.exempt_labels) or 'none'}{', dry run' if config.dry_run else ''}")

    issues = client.paginate(f"/repos/{repo}/issues", {"state": "open", "assignee": "*"})

    results = []
    failures = []
    for issue in issues:
        if "pull_request" in issue or not issue.get("assignees"):
            continue
        if has_exempt_label(issue, config.exempt_labels):
            log(f"#{issue['number']}: skipped, has an exempt label")
            continue
        try:
            results.append(process_issue(client, repo, issue, now, config, log))
        except Exception as error:  # keep going so one issue cannot block the others
            failures.append(issue["number"])
            warn(f"#{issue['number']}: {error}")
    return results, failures, config


def main(env=None):
    env = os.environ if env is None else env
    client = GitHubClient(env["GITHUB_TOKEN"], env.get("GITHUB_API_URL", "https://api.github.com"))
    try:
        results, failures, config = run(
            client, env["GITHUB_REPOSITORY"], env,
            warn=lambda message: print(f"::warning::{message}"))
    except ValueError as error:
        print(f"::error::{error}")
        return 1

    summary_file = env.get("GITHUB_STEP_SUMMARY")
    if summary_file:
        with open(summary_file, "a", encoding="utf-8") as summary:
            summary.write(build_summary(results, config))

    if failures:
        numbers = ", ".join(f"#{number}" for number in failures)
        print(f"::error::Could not process {len(failures)} issue(s): {numbers}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
