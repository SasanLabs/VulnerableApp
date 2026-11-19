const assert = require("node:assert/strict");
const { describe, it } = require("node:test");

const {
  buildReminderComment,
  buildUnassignedComment,
  evaluateAssignee,
  hasExemptLabel,
  parseConfig,
  reminderMarker,
  run,
} = require("./inactive-assignees.js");

const DAY = 24 * 60 * 60 * 1000;
const NOW = Date.parse("2026-11-20T12:00:00Z");
const CONFIG = { inactivityDays: 30, reminderIntervalDays: 10, exemptLabels: [], dryRun: false };

const BOT = { login: "github-actions[bot]", type: "Bot" };

function daysAgo(days) {
  return new Date(NOW - days * DAY).toISOString();
}

function assigned(login, days) {
  return { event: "assigned", assignee: { login }, actor: { login: "maintainer" }, created_at: daysAgo(days) };
}

function comment(login, days, body = "Working on it") {
  return { event: "commented", user: { login, type: "User" }, body, created_at: daysAgo(days) };
}

function reminder(login, days) {
  return { event: "commented", user: BOT, body: buildReminderComment([login], CONFIG), created_at: daysAgo(days) };
}

function crossReference(login, days, { pullRequest = false, state = "closed" } = {}) {
  return {
    event: "cross-referenced",
    actor: { login },
    created_at: daysAgo(days),
    source: {
      type: "issue",
      issue: {
        number: 1000,
        state,
        user: { login, type: "User" },
        ...(pullRequest ? { pull_request: { url: "https://example.test/pr/1000" } } : {}),
      },
    },
  };
}

describe("parseConfig", () => {
  it("uses the defaults when nothing is configured", () => {
    assert.deepEqual(parseConfig({}), {
      inactivityDays: 30,
      reminderIntervalDays: 10,
      exemptLabels: [],
      dryRun: false,
    });
  });

  it("reads the configured values", () => {
    assert.deepEqual(
      parseConfig({
        INACTIVITY_DAYS: "45",
        REMINDER_INTERVAL_DAYS: " 7 ",
        EXEMPT_LABELS: "pinned, do-not-close,,",
        DRY_RUN: "TRUE",
      }),
      { inactivityDays: 45, reminderIntervalDays: 7, exemptLabels: ["pinned", "do-not-close"], dryRun: true },
    );
  });

  it("rejects values that are not positive integers", () => {
    for (const value of ["0", "-3", "1.5", "ten"]) {
      assert.throws(() => parseConfig({ INACTIVITY_DAYS: value }), /INACTIVITY_DAYS must be a positive integer/);
    }
  });

  it("rejects a reminder interval that leaves no room for a reminder", () => {
    assert.throws(
      () => parseConfig({ INACTIVITY_DAYS: "10", REMINDER_INTERVAL_DAYS: "10" }),
      /must be smaller than INACTIVITY_DAYS/,
    );
  });
});

describe("evaluateAssignee", () => {
  it("does nothing while the assignment is recent", () => {
    const result = evaluateAssignee("alice", [assigned("alice", 9)], NOW, CONFIG);
    assert.equal(result.action, "none");
  });

  it("reminds once the reminder interval has passed without activity", () => {
    const result = evaluateAssignee("alice", [assigned("alice", 10)], NOW, CONFIG);
    assert.equal(result.action, "remind");
    assert.equal(result.idleDays, 10);
  });

  it("does not send duplicate reminders within the reminder interval", () => {
    const timeline = [assigned("alice", 15), reminder("alice", 5)];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "none");
  });

  it("sends another reminder after a further reminder interval", () => {
    const timeline = [assigned("alice", 20), reminder("alice", 10)];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "remind");
  });

  it("unassigns after the inactivity period when a reminder went unanswered", () => {
    const timeline = [assigned("alice", 30), reminder("alice", 20), reminder("alice", 10)];
    const result = evaluateAssignee("alice", timeline, NOW, CONFIG);
    assert.equal(result.action, "unassign");
    assert.equal(result.idleDays, 30);
  });

  it("tolerates a reminder posted by a scheduled run that started late", () => {
    const lateReminder = { ...reminder("alice", 10), created_at: new Date(NOW - 10 * DAY + 30 * 60 * 1000).toISOString() };
    const timeline = [assigned("alice", 30), reminder("alice", 20), lateReminder];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "unassign");
  });

  it("reminds before unassigning when an inactive assignee was never reminded", () => {
    const result = evaluateAssignee("alice", [assigned("alice", 90)], NOW, CONFIG);
    assert.equal(result.action, "remind");
  });

  it("waits a full reminder interval after the last reminder before unassigning", () => {
    const timeline = [assigned("alice", 90), reminder("alice", 9)];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "none");
  });

  it("restarts the inactivity period when the assignee comments", () => {
    const timeline = [assigned("alice", 40), reminder("alice", 30), reminder("alice", 20), comment("alice", 5)];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "none");
  });

  it("ignores reminders sent before the latest activity", () => {
    const timeline = [assigned("alice", 60), reminder("alice", 45), comment("Alice", 31)];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "remind");
  });

  it("does not count comments from other users or bots as activity", () => {
    const timeline = [
      assigned("alice", 40),
      reminder("alice", 15),
      comment("maintainer", 2, "Any update?"),
      { event: "commented", user: BOT, body: "Automated message", created_at: daysAgo(1) },
    ];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "unassign");
  });

  it("ignores reminder markers that were not posted by a bot", () => {
    const timeline = [
      assigned("alice", 40),
      comment("mallory", 20, reminderMarker("alice")),
    ];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "remind");
  });

  it("uses the latest assignment when an issue is assigned again", () => {
    const timeline = [assigned("alice", 100), reminder("alice", 80), assigned("alice", 3)];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "none");
  });

  it("counts referencing issues, commits and linked pull requests by the assignee as activity", () => {
    const base = [assigned("alice", 40), reminder("alice", 15)];
    const events = [
      crossReference("alice", 2),
      { event: "referenced", actor: { login: "alice" }, commit_id: "abc123", created_at: daysAgo(2) },
      { event: "connected", actor: { login: "alice" }, created_at: daysAgo(2) },
    ];
    for (const event of events) {
      assert.equal(evaluateAssignee("alice", [...base, event], NOW, CONFIG).action, "none", event.event);
    }
  });

  it("does not count references made by someone else", () => {
    const timeline = [assigned("alice", 40), reminder("alice", 15), crossReference("bob", 2)];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "unassign");
  });

  it("leaves assignees alone while they have an open pull request for the issue", () => {
    const timeline = [
      assigned("alice", 120),
      crossReference("alice", 100, { pullRequest: true, state: "open" }),
      reminder("alice", 50),
    ];
    const result = evaluateAssignee("alice", timeline, NOW, CONFIG);
    assert.equal(result.action, "none");
    assert.match(result.reason, /open pull request/);
  });

  it("does not treat a closed pull request as ongoing work", () => {
    const timeline = [
      assigned("alice", 120),
      crossReference("alice", 100, { pullRequest: true, state: "closed" }),
      reminder("alice", 50),
    ];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "unassign");
  });

  it("evaluates each assignee independently", () => {
    const timeline = [assigned("alice", 40), assigned("bob", 40), reminder("alice", 15), comment("bob", 1)];
    assert.equal(evaluateAssignee("alice", timeline, NOW, CONFIG).action, "unassign");
    assert.equal(evaluateAssignee("bob", timeline, NOW, CONFIG).action, "none");
  });

  it("does nothing when the assignment event cannot be found", () => {
    const result = evaluateAssignee("alice", [comment("alice", 60)], NOW, CONFIG);
    assert.equal(result.action, "none");
  });
});

describe("hasExemptLabel", () => {
  it("matches label names case-insensitively", () => {
    assert.equal(hasExemptLabel({ labels: [{ name: "Pinned" }] }, ["pinned"]), true);
    assert.equal(hasExemptLabel({ labels: ["do-not-close"] }, ["pinned", "do-not-close"]), true);
    assert.equal(hasExemptLabel({ labels: [{ name: "bug" }] }, ["pinned"]), false);
    assert.equal(hasExemptLabel({ labels: [{ name: "bug" }] }, []), false);
  });
});

describe("comments", () => {
  it("mentions every assignee and embeds a reminder marker for each of them", () => {
    const body = buildReminderComment(["alice", "Bob"], CONFIG);
    assert.match(body, /@alice, @Bob/);
    assert.ok(body.includes(reminderMarker("alice")));
    assert.ok(body.includes(reminderMarker("bob")));
    assert.match(body, /30 days/);
  });

  it("explains why assignees were unassigned", () => {
    assert.match(buildUnassignedComment(["alice"], CONFIG), /^@alice has been unassigned .* 30 days/);
    assert.match(buildUnassignedComment(["alice", "bob"], CONFIG), /^@alice, @bob have been unassigned/);
  });
});

describe("run", () => {
  function fakeGitHub({ issues, timelines }) {
    const calls = { removeAssignees: [], createComment: [] };
    const rest = {
      issues: {
        listForRepo: Symbol("listForRepo"),
        listEventsForTimeline: Symbol("listEventsForTimeline"),
        removeAssignees: async (params) => calls.removeAssignees.push(params),
        createComment: async (params) => calls.createComment.push(params),
      },
    };
    const paginate = async (method, params) => {
      if (method === rest.issues.listForRepo) {
        assert.deepEqual(params, { owner: "SasanLabs", repo: "VulnerableApp", state: "open", assignee: "*", per_page: 100 });
        return issues;
      }
      if (method === rest.issues.listEventsForTimeline) {
        const timeline = timelines[params.issue_number];
        if (timeline instanceof Error) {
          throw timeline;
        }
        return timeline;
      }
      throw new Error("unexpected method");
    };
    return { github: { rest, paginate }, calls };
  }

  function fakeCore() {
    const core = { infos: [], warnings: [], failed: null, tables: [] };
    core.info = (message) => core.infos.push(message);
    core.warning = (message) => core.warnings.push(message);
    core.setFailed = (message) => {
      core.failed = message;
    };
    core.summary = {
      addHeading: () => core.summary,
      addRaw: () => core.summary,
      addTable: (table) => {
        core.tables.push(table);
        return core.summary;
      },
      write: async () => core.summary,
    };
    return core;
  }

  const context = { repo: { owner: "SasanLabs", repo: "VulnerableApp" } };

  function issue(number, logins, labels = []) {
    return {
      number,
      html_url: `https://github.com/SasanLabs/VulnerableApp/issues/${number}`,
      assignees: logins.map((login) => ({ login })),
      labels: labels.map((name) => ({ name })),
    };
  }

  it("reminds, unassigns and skips issues as configured", async () => {
    const { github, calls } = fakeGitHub({
      issues: [
        issue(1, ["alice", "bob"]),
        issue(2, ["carol"]),
        issue(3, ["dave"], ["pinned"]),
        { ...issue(4, ["erin"]), pull_request: {} },
      ],
      timelines: {
        1: [assigned("alice", 40), reminder("alice", 15), assigned("bob", 12)],
        2: [assigned("carol", 2)],
      },
    });
    const core = fakeCore();

    const results = await run({ github, context, core, env: { EXEMPT_LABELS: "pinned" }, now: NOW });

    assert.deepEqual(results.map((r) => [r.number, r.toUnassign, r.toRemind]), [
      [1, ["alice"], ["bob"]],
      [2, [], []],
    ]);
    assert.deepEqual(calls.removeAssignees, [
      { owner: "SasanLabs", repo: "VulnerableApp", issue_number: 1, assignees: ["alice"] },
    ]);
    assert.equal(calls.createComment.length, 2);
    assert.match(calls.createComment[0].body, /@alice has been unassigned/);
    assert.match(calls.createComment[1].body, /Hi @bob,/);
    assert.ok(calls.createComment[1].body.includes(reminderMarker("bob")));
    assert.equal(core.tables[0].length, 3);
    assert.equal(core.failed, null);
  });

  it("makes no changes in dry run mode", async () => {
    const { github, calls } = fakeGitHub({
      issues: [issue(1, ["alice"])],
      timelines: { 1: [assigned("alice", 40), reminder("alice", 15)] },
    });

    const results = await run({ github, context, core: fakeCore(), env: { DRY_RUN: "true" }, now: NOW });

    assert.deepEqual(results[0].toUnassign, ["alice"]);
    assert.deepEqual(calls, { removeAssignees: [], createComment: [] });
  });

  it("keeps processing other issues and fails the run when an issue cannot be processed", async () => {
    const { github, calls } = fakeGitHub({
      issues: [issue(1, ["alice"]), issue(2, ["bob"])],
      timelines: { 1: new Error("boom"), 2: [assigned("bob", 10)] },
    });
    const core = fakeCore();

    await run({ github, context, core, env: {}, now: NOW });

    assert.equal(calls.createComment.length, 1);
    assert.equal(calls.createComment[0].issue_number, 2);
    assert.deepEqual(core.warnings, ["#1: boom"]);
    assert.match(core.failed, /#1/);
  });
});
