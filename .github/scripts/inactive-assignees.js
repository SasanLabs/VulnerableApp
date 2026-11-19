// Reminds assignees of inactive issues and unassigns them once an issue has had
// no activity from them for a configurable number of days.
//
// The module is executed by .github/workflows/inactive-assignees.yml through
// actions/github-script. The decision logic is kept in pure functions so it can
// be unit tested with `node --test .github/scripts/inactive-assignees.test.js`.

const DAY_IN_MS = 24 * 60 * 60 * 1000;
// Scheduled workflows often start a little later than their cron time. Without
// some slack, a reminder posted by a delayed run would look a few minutes short
// of a full interval on the run exactly one interval later.
const SCHEDULE_TOLERANCE_MS = 2 * 60 * 60 * 1000;

const REMINDER_MARKER_PREFIX = "inactive-assignee-reminder";

const DEFAULT_CONFIG = Object.freeze({
  inactivityDays: 30,
  reminderIntervalDays: 10,
  exemptLabels: [],
  dryRun: false,
});

function reminderMarker(login) {
  return `<!-- ${REMINDER_MARKER_PREFIX}: ${login.toLowerCase()} -->`;
}

function parsePositiveInteger(name, value, defaultValue) {
  if (value === undefined || value === null || String(value).trim() === "") {
    return defaultValue;
  }
  const text = String(value).trim();
  const parsed = Number(text);
  if (!/^\d+$/.test(text) || parsed < 1) {
    throw new Error(`${name} must be a positive integer, got "${value}"`);
  }
  return parsed;
}

function parseList(value) {
  if (!value) {
    return [];
  }
  return String(value)
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function parseConfig(env) {
  const inactivityDays = parsePositiveInteger(
    "INACTIVITY_DAYS",
    env.INACTIVITY_DAYS,
    DEFAULT_CONFIG.inactivityDays,
  );
  const reminderIntervalDays = parsePositiveInteger(
    "REMINDER_INTERVAL_DAYS",
    env.REMINDER_INTERVAL_DAYS,
    DEFAULT_CONFIG.reminderIntervalDays,
  );
  if (reminderIntervalDays >= inactivityDays) {
    throw new Error(
      `REMINDER_INTERVAL_DAYS (${reminderIntervalDays}) must be smaller than ` +
        `INACTIVITY_DAYS (${inactivityDays}), otherwise no reminder would be ` +
        "sent before an assignee is unassigned",
    );
  }
  return {
    inactivityDays,
    reminderIntervalDays,
    exemptLabels: parseList(env.EXEMPT_LABELS),
    dryRun: String(env.DRY_RUN || "").trim().toLowerCase() === "true",
  };
}

function sameLogin(a, b) {
  return Boolean(a) && Boolean(b) && a.toLowerCase() === b.toLowerCase();
}

function isBot(user) {
  if (!user || !user.login) {
    return false;
  }
  return user.type === "Bot" || user.login.endsWith("[bot]");
}

function timestamp(value) {
  const time = Date.parse(value);
  return Number.isNaN(time) ? null : time;
}

function isOpenPullRequestBy(event, login) {
  const issue = event.source && event.source.issue;
  return (
    Boolean(issue) &&
    Boolean(issue.pull_request) &&
    issue.state === "open" &&
    Boolean(issue.user) &&
    sameLogin(issue.user.login, login)
  );
}

// Returns true when the timeline event is progress made by the assignee:
// a comment, an issue or pull request referencing this issue, a commit
// referencing it, or a pull request linked through the Development sidebar.
// Comments and events from bots, including this automation, never count.
function isActivityBy(event, login) {
  switch (event.event) {
    case "commented":
      return !isBot(event.user) && sameLogin(event.user && event.user.login, login);
    case "cross-referenced": {
      const issue = event.source && event.source.issue;
      return Boolean(issue) && !isBot(issue.user) && sameLogin(issue.user && issue.user.login, login);
    }
    case "referenced":
    case "connected":
      return !isBot(event.actor) && sameLogin(event.actor && event.actor.login, login);
    default:
      return false;
  }
}

function isMarkerCommentFor(event, marker) {
  return (
    event.event === "commented" &&
    isBot(event.user) &&
    typeof event.body === "string" &&
    event.body.includes(marker)
  );
}

// Works out what should happen to a single assignee of an issue, based on the
// issue timeline. The result's `action` is one of:
//   "none"     - nothing to do (recently active, already reminded, or has an
//                open pull request for the issue)
//   "remind"   - post a reminder asking for a progress update
//   "unassign" - remove the assignee and explain why
function evaluateAssignee(login, timeline, now, config) {
  let assignedAt = null;
  let lastActivityAt = null;
  let lastReminderAt = null;
  let hasOpenPullRequest = false;

  for (const event of timeline) {
    const time = timestamp(event.created_at);
    if (event.event === "cross-referenced" && isOpenPullRequestBy(event, login)) {
      hasOpenPullRequest = true;
    }
    if (time === null) {
      continue;
    }
    if (event.event === "assigned" && sameLogin(event.assignee && event.assignee.login, login)) {
      assignedAt = Math.max(assignedAt || 0, time);
    } else if (isActivityBy(event, login)) {
      lastActivityAt = Math.max(lastActivityAt || 0, time);
    } else if (isMarkerCommentFor(event, reminderMarker(login))) {
      lastReminderAt = Math.max(lastReminderAt || 0, time);
    }
  }

  if (assignedAt === null) {
    return { login, action: "none", reason: "no assignment event found" };
  }
  if (hasOpenPullRequest) {
    return { login, action: "none", reason: "has an open pull request for this issue" };
  }

  const activeSince = Math.max(assignedAt, lastActivityAt || 0);
  const idleDays = Math.floor((now - activeSince) / DAY_IN_MS);
  // Only reminders sent after the latest activity matter: any activity from the
  // assignee starts the inactivity period again.
  const remindedAt = lastReminderAt !== null && lastReminderAt > activeSince ? lastReminderAt : null;
  const daysSinceReminder =
    remindedAt === null
      ? null
      : Math.floor((now - remindedAt + SCHEDULE_TOLERANCE_MS) / DAY_IN_MS);

  // An assignee is unassigned only after an unanswered reminder has been on the
  // issue for a full reminder interval, so nobody loses an issue without warning.
  if (
    idleDays >= config.inactivityDays &&
    daysSinceReminder !== null &&
    daysSinceReminder >= config.reminderIntervalDays
  ) {
    return { login, action: "unassign", idleDays };
  }
  if (
    idleDays >= config.reminderIntervalDays &&
    (daysSinceReminder === null || daysSinceReminder >= config.reminderIntervalDays)
  ) {
    return { login, action: "remind", idleDays };
  }
  return { login, action: "none", idleDays, reason: "recently active or already reminded" };
}

function hasExemptLabel(issue, exemptLabels) {
  const wanted = exemptLabels.map((label) => label.toLowerCase());
  return (issue.labels || []).some((label) => {
    const name = typeof label === "string" ? label : label && label.name;
    return Boolean(name) && wanted.includes(name.toLowerCase());
  });
}

function mentionList(logins) {
  return logins.map((login) => `@${login}`).join(", ");
}

function buildReminderComment(logins, config) {
  return [
    ...logins.map(reminderMarker),
    `Hi ${mentionList(logins)}, this issue is assigned to you but has had no activity from you ` +
      `for at least ${config.reminderIntervalDays} days.`,
    "",
    "Could you share a short update on your progress? If you are no longer working on it, " +
      "please leave a comment so that someone else can pick it up.",
    "",
    `Assignees who have no activity on an issue for ${config.inactivityDays} days are ` +
      "unassigned automatically. Commenting here, or opening a pull request that references " +
      "this issue, resets the timer.",
  ].join("\n");
}

function buildUnassignedComment(logins, config) {
  return [
    `${mentionList(logins)} ${logins.length === 1 ? "has" : "have"} been unassigned from this ` +
      `issue because there was no activity on it from them for at least ` +
      `${config.inactivityDays} days, even after a reminder.`,
    "",
    "This issue is now available for other contributors to pick up. If you are still working " +
      "on it, leave a comment and a maintainer can assign it back to you.",
  ].join("\n");
}

async function processIssue({ github, owner, repo, issue, now, config, core }) {
  const timeline = await github.paginate(github.rest.issues.listEventsForTimeline, {
    owner,
    repo,
    issue_number: issue.number,
    per_page: 100,
  });

  const decisions = issue.assignees.map((assignee) =>
    evaluateAssignee(assignee.login, timeline, now, config),
  );
  const toUnassign = decisions.filter((d) => d.action === "unassign").map((d) => d.login);
  const toRemind = decisions.filter((d) => d.action === "remind").map((d) => d.login);

  for (const decision of decisions) {
    core.info(
      `#${issue.number} @${decision.login}: ${decision.action}` +
        (decision.idleDays !== undefined ? ` (inactive for ${decision.idleDays} days)` : "") +
        (decision.reason ? ` - ${decision.reason}` : ""),
    );
  }

  if (!config.dryRun) {
    if (toUnassign.length > 0) {
      await github.rest.issues.removeAssignees({
        owner,
        repo,
        issue_number: issue.number,
        assignees: toUnassign,
      });
      await github.rest.issues.createComment({
        owner,
        repo,
        issue_number: issue.number,
        body: buildUnassignedComment(toUnassign, config),
      });
    }
    if (toRemind.length > 0) {
      await github.rest.issues.createComment({
        owner,
        repo,
        issue_number: issue.number,
        body: buildReminderComment(toRemind, config),
      });
    }
  }

  return { number: issue.number, url: issue.html_url, toUnassign, toRemind };
}

async function run({ github, context, core, env = process.env, now = Date.now() }) {
  const config = parseConfig(env);
  const { owner, repo } = context.repo;
  core.info(
    `Inactivity period: ${config.inactivityDays} days, reminder interval: ` +
      `${config.reminderIntervalDays} days, exempt labels: ` +
      `${config.exemptLabels.join(", ") || "none"}${config.dryRun ? ", dry run" : ""}`,
  );

  const issues = await github.paginate(github.rest.issues.listForRepo, {
    owner,
    repo,
    state: "open",
    assignee: "*",
    per_page: 100,
  });

  const results = [];
  const failures = [];
  for (const issue of issues) {
    if (issue.pull_request || !issue.assignees || issue.assignees.length === 0) {
      continue;
    }
    if (hasExemptLabel(issue, config.exemptLabels)) {
      core.info(`#${issue.number}: skipped, has an exempt label`);
      continue;
    }
    try {
      results.push(await processIssue({ github, owner, repo, issue, now, config, core }));
    } catch (error) {
      failures.push(issue.number);
      core.warning(`#${issue.number}: ${error.message}`);
    }
  }

  const unassignLabel = config.dryRun ? "Would be unassigned" : "Unassigned";
  const remindLabel = config.dryRun ? "Would be reminded" : "Reminded";
  const rows = results.flatMap((result) => [
    ...result.toUnassign.map((login) => [`#${result.number}`, `@${login}`, unassignLabel]),
    ...result.toRemind.map((login) => [`#${result.number}`, `@${login}`, remindLabel]),
  ]);
  const heading = config.dryRun ? "Inactive assignees (dry run, no changes made)" : "Inactive assignees";
  core.summary.addHeading(heading, 2);
  if (rows.length === 0) {
    core.summary.addRaw("No assignee needed a reminder or had to be unassigned.");
  } else {
    core.summary.addTable([
      [
        { data: "Issue", header: true },
        { data: "Assignee", header: true },
        { data: "Action", header: true },
      ],
      ...rows,
    ]);
  }
  await core.summary.write();

  if (failures.length > 0) {
    core.setFailed(
      `Could not process ${failures.length} issue(s): ${failures.map((n) => `#${n}`).join(", ")}`,
    );
  }
  return results;
}

module.exports = {
  DEFAULT_CONFIG,
  buildReminderComment,
  buildUnassignedComment,
  evaluateAssignee,
  hasExemptLabel,
  parseConfig,
  reminderMarker,
  run,
};
