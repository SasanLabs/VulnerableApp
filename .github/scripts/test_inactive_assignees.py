import os
import tempfile
import unittest
from datetime import datetime, timedelta, timezone

import inactive_assignees as ia

NOW = datetime(2026, 11, 20, 12, 0, tzinfo=timezone.utc)
CONFIG = ia.Config()
BOT = {"login": "github-actions[bot]", "type": "Bot"}


def days_ago(days):
    return (NOW - timedelta(days=days)).isoformat()


def assigned(login, days):
    return {"event": "assigned", "assignee": {"login": login},
            "actor": {"login": "maintainer"}, "created_at": days_ago(days)}


def comment(login, days, body="Working on it"):
    return {"event": "commented", "user": {"login": login, "type": "User"},
            "body": body, "created_at": days_ago(days)}


def reminder(login, days):
    return {"event": "commented", "user": BOT,
            "body": ia.build_reminder_comment([login], CONFIG), "created_at": days_ago(days)}


def cross_reference(login, days, pull_request=False, state="closed", author=None):
    issue = {"number": 1000, "state": state,
             "user": {"login": author or login, "type": "User"}}
    if pull_request:
        issue["pull_request"] = {"url": "https://example.test/pr/1000"}
    return {"event": "cross-referenced", "actor": {"login": login, "type": "User"},
            "created_at": days_ago(days), "source": {"type": "issue", "issue": issue}}


def evaluate(login, timeline, authors=()):
    return ia.evaluate_assignee(login, timeline, NOW, CONFIG, authors)


class ParseConfigTest(unittest.TestCase):
    def test_uses_the_defaults_when_nothing_is_configured(self):
        self.assertEqual(ia.parse_config({}), ia.Config(30, 10, (), False))

    def test_reads_the_configured_values(self):
        config = ia.parse_config({
            "INACTIVITY_DAYS": "45", "REMINDER_INTERVAL_DAYS": " 7 ",
            "EXEMPT_LABELS": "pinned, do-not-close,,", "DRY_RUN": "TRUE"})
        self.assertEqual(config, ia.Config(45, 7, ("pinned", "do-not-close"), True))

    def test_rejects_values_that_are_not_positive_integers(self):
        for value in ["0", "-3", "1.5", "ten"]:
            with self.assertRaisesRegex(ValueError, "INACTIVITY_DAYS must be a positive integer"):
                ia.parse_config({"INACTIVITY_DAYS": value})

    def test_rejects_a_reminder_interval_that_leaves_no_room_for_a_reminder(self):
        with self.assertRaisesRegex(ValueError, "must be smaller than INACTIVITY_DAYS"):
            ia.parse_config({"INACTIVITY_DAYS": "10", "REMINDER_INTERVAL_DAYS": "10"})


class EvaluateAssigneeTest(unittest.TestCase):
    def test_does_nothing_while_the_assignment_is_recent(self):
        self.assertEqual(evaluate("alice", [assigned("alice", 9)]).action, "none")

    def test_reminds_once_the_reminder_interval_has_passed_without_activity(self):
        result = evaluate("alice", [assigned("alice", 10)])
        self.assertEqual((result.action, result.idle_days), ("remind", 10))

    def test_does_not_send_duplicate_reminders_within_the_interval(self):
        self.assertEqual(
            evaluate("alice", [assigned("alice", 15), reminder("alice", 5)]).action, "none")

    def test_sends_another_reminder_after_a_further_interval(self):
        self.assertEqual(
            evaluate("alice", [assigned("alice", 20), reminder("alice", 10)]).action, "remind")

    def test_unassigns_after_the_inactivity_period_when_a_reminder_went_unanswered(self):
        timeline = [assigned("alice", 30), reminder("alice", 20), reminder("alice", 10)]
        result = evaluate("alice", timeline)
        self.assertEqual((result.action, result.idle_days), ("unassign", 30))

    def test_tolerates_a_reminder_posted_by_a_scheduled_run_that_started_late(self):
        late = {**reminder("alice", 10),
                "created_at": (NOW - timedelta(days=10) + timedelta(minutes=30)).isoformat()}
        timeline = [assigned("alice", 30), reminder("alice", 20), late]
        self.assertEqual(evaluate("alice", timeline).action, "unassign")

    def test_reminds_before_unassigning_when_an_inactive_assignee_was_never_reminded(self):
        self.assertEqual(evaluate("alice", [assigned("alice", 90)]).action, "remind")

    def test_waits_a_full_interval_after_the_last_reminder_before_unassigning(self):
        timeline = [assigned("alice", 90), reminder("alice", 9)]
        self.assertEqual(evaluate("alice", timeline).action, "none")

    def test_restarts_the_inactivity_period_when_the_assignee_comments(self):
        timeline = [assigned("alice", 40), reminder("alice", 30), reminder("alice", 20),
                    comment("alice", 5)]
        self.assertEqual(evaluate("alice", timeline).action, "none")

    def test_ignores_reminders_sent_before_the_latest_activity(self):
        timeline = [assigned("alice", 60), reminder("alice", 45), comment("Alice", 31)]
        self.assertEqual(evaluate("alice", timeline).action, "remind")

    def test_does_not_count_comments_from_other_users_or_bots_as_activity(self):
        timeline = [
            assigned("alice", 40), reminder("alice", 15),
            comment("maintainer", 2, "Any update?"),
            {"event": "commented", "user": BOT, "body": "Automated message",
             "created_at": days_ago(1)},
        ]
        self.assertEqual(evaluate("alice", timeline).action, "unassign")

    def test_ignores_reminder_markers_that_were_not_posted_by_a_bot(self):
        timeline = [assigned("alice", 40), comment("mallory", 20, ia.reminder_marker("alice"))]
        self.assertEqual(evaluate("alice", timeline).action, "remind")

    def test_uses_the_latest_assignment_when_an_issue_is_assigned_again(self):
        timeline = [assigned("alice", 100), reminder("alice", 80), assigned("alice", 3)]
        self.assertEqual(evaluate("alice", timeline).action, "none")

    def test_counts_references_commits_and_linked_pull_requests_as_activity(self):
        base = [assigned("alice", 40), reminder("alice", 15)]
        events = [
            cross_reference("alice", 2),
            {"event": "referenced", "actor": {"login": "alice"}, "commit_id": "abc123",
             "created_at": days_ago(2)},
            {"event": "connected", "actor": {"login": "alice"}, "created_at": days_ago(2)},
        ]
        for event in events:
            with self.subTest(event=event["event"]):
                self.assertEqual(evaluate("alice", [*base, event]).action, "none")

    def test_does_not_count_references_made_by_someone_else(self):
        timeline = [assigned("alice", 40), reminder("alice", 15), cross_reference("bob", 2)]
        self.assertEqual(evaluate("alice", timeline).action, "unassign")

    def test_attributes_a_reference_to_whoever_wrote_it(self):
        base = [assigned("alice", 40), reminder("alice", 15)]
        by_bob_in_alices_issue = cross_reference("bob", 2, author="alice")
        by_alice_in_bobs_issue = cross_reference("alice", 2, author="bob")
        self.assertEqual(evaluate("alice", [*base, by_bob_in_alices_issue]).action, "unassign")
        self.assertEqual(evaluate("alice", [*base, by_alice_in_bobs_issue]).action, "none")

    def test_leaves_assignees_alone_while_an_open_linked_pull_request_is_theirs(self):
        timeline = [assigned("alice", 90),
                    {"event": "connected", "actor": {"login": "alice"}, "created_at": days_ago(40)},
                    reminder("alice", 15)]
        self.assertEqual(evaluate("alice", timeline, ["bob"]).action, "unassign")
        result = evaluate("alice", timeline, ["Alice"])
        self.assertEqual(result.action, "none")
        self.assertIn("open pull request", result.reason)

    def test_leaves_assignees_alone_while_they_have_an_open_pull_request_for_the_issue(self):
        timeline = [assigned("alice", 120),
                    cross_reference("alice", 100, pull_request=True, state="open"),
                    reminder("alice", 50)]
        result = evaluate("alice", timeline)
        self.assertEqual(result.action, "none")
        self.assertIn("open pull request", result.reason)

    def test_does_not_treat_a_closed_pull_request_as_ongoing_work(self):
        timeline = [assigned("alice", 120),
                    cross_reference("alice", 100, pull_request=True, state="closed"),
                    reminder("alice", 50)]
        self.assertEqual(evaluate("alice", timeline).action, "unassign")

    def test_evaluates_each_assignee_independently(self):
        timeline = [assigned("alice", 40), assigned("bob", 40), reminder("alice", 15),
                    comment("bob", 1)]
        self.assertEqual(evaluate("alice", timeline).action, "unassign")
        self.assertEqual(evaluate("bob", timeline).action, "none")

    def test_does_nothing_when_the_assignment_event_cannot_be_found(self):
        self.assertEqual(evaluate("alice", [comment("alice", 60)]).action, "none")


class HasExemptLabelTest(unittest.TestCase):
    def test_matches_label_names_case_insensitively(self):
        self.assertTrue(ia.has_exempt_label({"labels": [{"name": "Pinned"}]}, ["pinned"]))
        self.assertTrue(ia.has_exempt_label({"labels": ["do-not-close"]}, ["pinned", "do-not-close"]))
        self.assertFalse(ia.has_exempt_label({"labels": [{"name": "bug"}]}, ["pinned"]))
        self.assertFalse(ia.has_exempt_label({"labels": [{"name": "bug"}]}, []))


class CommentsTest(unittest.TestCase):
    def test_reminder_mentions_every_assignee_and_embeds_a_marker_for_each(self):
        body = ia.build_reminder_comment(["alice", "Bob"], CONFIG)
        self.assertIn("@alice, @Bob", body)
        self.assertIn(ia.reminder_marker("alice"), body)
        self.assertIn(ia.reminder_marker("bob"), body)
        self.assertIn("30 days", body)

    def test_unassigned_comment_explains_why(self):
        self.assertRegex(ia.build_unassigned_comment(["alice"], CONFIG),
                         r"^@alice has been unassigned .* 30 days")
        self.assertTrue(ia.build_unassigned_comment(["alice", "bob"], CONFIG)
                        .startswith("@alice, @bob have been unassigned"))


class FakeClient:
    def __init__(self, issues, timelines, linked_pull_requests=None):
        self.issues = issues
        self.timelines = timelines
        self.linked = linked_pull_requests or {}
        self.removed = []
        self.comments = []

    def paginate(self, path, params=None):
        if path == "/repos/SasanLabs/VulnerableApp/issues":
            assert params == {"state": "open", "assignee": "*"}
            return self.issues
        number = int(path.split("/")[-2])
        timeline = self.timelines[number]
        if isinstance(timeline, Exception):
            raise timeline
        return timeline

    def graphql(self, query, variables):
        assert "closedByPullRequestsReferences(first: 100, includeClosedPrs: false)" in query
        assert (variables["owner"], variables["repo"]) == ("SasanLabs", "VulnerableApp")
        nodes = self.linked.get(variables["number"], [])
        return {"repository": {"issue": {"closedByPullRequestsReferences": {"nodes": nodes}}}}

    def remove_assignees(self, repo, number, logins):
        self.removed.append((number, logins))

    def create_comment(self, repo, number, body):
        self.comments.append((number, body))


def issue(number, logins, labels=()):
    return {"number": number, "assignees": [{"login": login} for login in logins],
            "labels": [{"name": name} for name in labels]}


class RunTest(unittest.TestCase):
    REPO = "SasanLabs/VulnerableApp"

    def test_reminds_unassigns_and_skips_issues_as_configured(self):
        client = FakeClient(
            issues=[
                issue(1, ["alice", "bob"]),
                issue(2, ["carol"]),
                issue(3, ["dave"], ["pinned"]),
                {**issue(4, ["erin"]), "pull_request": {}},
                issue(5, ["frank"]),
            ],
            timelines={
                1: [assigned("alice", 40), reminder("alice", 15), assigned("bob", 12)],
                2: [assigned("carol", 2)],
                5: [assigned("frank", 60), reminder("frank", 20)],
            },
            linked_pull_requests={
                1: [{"state": "OPEN", "author": {"login": "someone-else"}}],
                5: [{"state": "MERGED", "author": {"login": "frank"}},
                    {"state": "OPEN", "author": {"login": "frank"}}],
            })

        results, failures, _ = ia.run(client, self.REPO, {"EXEMPT_LABELS": "pinned"},
                                      now=NOW, log=lambda _: None)

        self.assertEqual([(r.number, r.to_unassign, r.to_remind) for r in results],
                         [(1, ["alice"], ["bob"]), (2, [], []), (5, [], [])])
        self.assertEqual(failures, [])
        self.assertEqual(client.removed, [(1, ["alice"])])
        self.assertEqual(len(client.comments), 2)
        self.assertIn("@alice has been unassigned", client.comments[0][1])
        self.assertIn("Hi @bob,", client.comments[1][1])
        self.assertIn(ia.reminder_marker("bob"), client.comments[1][1])

    def test_makes_no_changes_in_dry_run_mode(self):
        client = FakeClient([issue(1, ["alice"])],
                            {1: [assigned("alice", 40), reminder("alice", 15)]})
        results, _, config = ia.run(client, self.REPO, {"DRY_RUN": "true"},
                                    now=NOW, log=lambda _: None)
        self.assertEqual(results[0].to_unassign, ["alice"])
        self.assertEqual((client.removed, client.comments), ([], []))
        self.assertIn("Would be unassigned", ia.build_summary(results, config))

    def test_keeps_processing_other_issues_when_one_cannot_be_processed(self):
        client = FakeClient([issue(1, ["alice"]), issue(2, ["bob"])],
                            {1: RuntimeError("boom"), 2: [assigned("bob", 10)]})
        warnings = []
        _, failures, _ = ia.run(client, self.REPO, {}, now=NOW,
                                log=lambda _: None, warn=warnings.append)
        self.assertEqual(failures, [1])
        self.assertEqual(warnings, ["#1: boom"])
        self.assertEqual([number for number, _ in client.comments], [2])

    def test_main_fails_the_run_and_writes_a_summary(self):
        client = FakeClient([issue(1, ["alice"])], {1: RuntimeError("boom")})
        original = ia.GitHubClient
        ia.GitHubClient = lambda *args, **kwargs: client
        try:
            with tempfile.TemporaryDirectory() as directory:
                path = os.path.join(directory, "summary.md")
                env = {"GITHUB_TOKEN": "x", "GITHUB_REPOSITORY": self.REPO,
                       "GITHUB_STEP_SUMMARY": path}
                self.assertEqual(ia.main(env), 1)
                with open(path, encoding="utf-8") as summary:
                    self.assertIn("No assignee needed", summary.read())
        finally:
            ia.GitHubClient = original

    def test_main_rejects_an_invalid_configuration(self):
        env = {"GITHUB_TOKEN": "x", "GITHUB_REPOSITORY": self.REPO, "INACTIVITY_DAYS": "abc"}
        self.assertEqual(ia.main(env), 1)


class PaginationTest(unittest.TestCase):
    def test_follows_next_links(self):
        client = ia.GitHubClient("token")
        pages = iter([
            ([1, 2], {"Link": '<https://api.github.com/next>; rel="next", <https://x>; rel="last"'}),
            ([3], {}),
        ])
        urls = []

        def fake_request(method, url, payload=None):
            urls.append(url)
            return next(pages)

        client._request = fake_request
        self.assertEqual(client.paginate("/repos/o/r/issues", {"state": "open"}), [1, 2, 3])
        self.assertEqual(urls[0], "https://api.github.com/repos/o/r/issues?state=open&per_page=100")
        self.assertEqual(urls[1], "https://api.github.com/next")


if __name__ == "__main__":
    unittest.main()
