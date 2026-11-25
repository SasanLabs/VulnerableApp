# Contributing to OWASP VulnerableApp
VulnerableApp is a community project, and we are always delighted to welcome new contributors!

There are lots of ways you can contribute:

## Got a Question or Problem?
If you have a question or problem related to using VulnerableApp then the first thing to do is to raise an issue on the VulnerableApp repo: https://github.com/SasanLabs/VulnerableApp/issues
or send an email to karan.sasan@owasp.org 

## Found an Issue?
If you have found a bug then raise an issue on the VulnerableApp repo: https://github.com/SasanLabs/VulnerableApp/issues

## Have a Feature Request?
If you have a suggestion for new functionality then you can raise an issue on the VulnerableApp repo: https://github.com/SasanLabs/VulnerableApp/issues

Its worth checking to see if its already been requested, and including as much information as you can so that we can fully understand your requirements.

### Adding code to VulnerableApp
Fixing [issues](https://github.com/SasanLabs/VulnerableApp/issues) is very valuable (ones flagged as [Good First issue](https://github.com/SasanLabs/VulnerableApp/issues?q=is%3Aopen+is%3Aissue++label%3A%22good+first+issue%22+) are good ones to start on) and there are always many improvements we want to make.

For more understanding on VulnerableApp's internal framework please visit: [Design Documentation](https://sasanlabs.github.io/VulnerableApp/DesignDocumentation.html)
#### Guidelines for Pull Request (PR) submission and processing:

##### Format/Style & Code Quality Validation

The Java code is formatted according to Google Java Style (AOSP variant) and validated for code quality and maintainability rules.

- **Spotless**: Checks code formatting using Spotless (which delegates to Google Java Format). You can automatically format your code locally with:
  ```bash
  ./gradlew spotlessApply
  ```
- **Checkstyle**: Validates maintainability rules such as naming conventions, import hygiene, modifier order, and Java code-quality standards. You can run Checkstyle checks locally with:
  ```bash
  ./gradlew checkstyleMain checkstyleTest
  ```
Both Checkstyle and Spotless checks are verified in CI for every pull request and push to `master`.

##### What should you, the author of a pull request, expect from us (VulnerableApp Team)?
* How much time (maximum) until the first feedback? 1 week.
* And following iterations? 1 week.
* This is a guideline we should normally be able to hit. If it's been more than a week and you haven't heard then please feel free to add a comment to your PR.

##### Issue assignment and inactivity
To keep issues moving and make sure no issue stays blocked by an assignment nobody is working on, the [Remind and unassign inactive assignees](workflows/inactive-assignees.yml) workflow runs every day and checks all open issues that have an assignee:

* If you have had no activity on an issue assigned to you for 10 days, you will get a reminder asking for a short progress update. Further reminders follow every 10 days while the issue stays inactive.
* If you still have no activity 30 days after you were assigned or last active, you will be unassigned and a comment will explain that the issue is available for other contributors. You are only unassigned after a reminder has gone unanswered for at least 10 days.
* Commenting on the issue, opening an issue or pull request that references it, pushing a commit that references it, or linking a pull request to it counts as activity and restarts the timer. Comments from other people and from bots do not.
* While you have an open pull request that references the issue or is linked to it, you are never reminded or unassigned, so waiting for a review is not counted against you.
* Issues labelled `pinned` or `do-not-close` are skipped.

If you are unassigned but are still working on the issue, leave a comment and a maintainer can assign it back to you.

Maintainers can change the inactivity period (`INACTIVITY_DAYS`), the reminder interval (`REMINDER_INTERVAL_DAYS`) and the exempt labels (`EXEMPT_LABELS`) in the workflow file. Running the workflow manually from the Actions tab does a dry run by default, which only lists the reminders and unassignments it would make in the run summary.
