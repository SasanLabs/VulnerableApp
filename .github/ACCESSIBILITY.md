# Accessibility

OWASP VulnerableApp is a deliberately vulnerable application used to test security tools and to support security learning, research, and education. Accessibility matters because people use different devices, input methods, and assistive technologies. We want users—including security engineers, researchers, educators, and students—and contributors to be able to explore the project, understand its documentation, and participate in its development with as few barriers as possible.

This statement describes the accessibility improvements we aim to make, what contributors should consider, how to report barriers, and what is currently known about accessibility support. **It is not a claim of conformance with any Web Content Accessibility Guidelines (WCAG) level.** No complete accessibility evaluation is available to cite in this statement.

## Priorities

We aim to make the project's user interfaces and documentation easier to use, especially in these areas:

- **Keyboard access:** Important navigation, forms, and actions should work without a mouse. Keyboard focus should follow a logical order and remain visible.
- **Screen reader access:** Headings, links, buttons, form fields, and status messages should have meaningful names and structure.
- **Readable content:** Instructions and error messages should be clear. Text should be distinguishable from its background, and important information should not be conveyed by color alone.
- **Language and documentation:** Use plain language and improve access to instructions for people who use different languages.
- **Consistent tasks across interfaces:** Check important tasks in both the Legacy UI and the Modern UI. Record barriers and work toward reducing them.

These are improvement goals, not guarantees about current behavior. Changes can be checked manually and any barriers tracked in GitHub Issues. If the project adopts a specific WCAG level as a target, it should be described as a goal until an evaluation verifies conformance. A verified claim would need to identify the tested scope, date, methods, and evaluator.

## Contributor expectations

When changing a user interface, interaction, user-facing message, or documentation, contributors should, where relevant:

- Use elements with appropriate meaning and provide clear names for buttons and form fields. Add alternative text for informative images; avoid unnecessary descriptions for purely decorative images.
- Check that affected tasks work with a keyboard, including focus order, visible focus, and appropriate use of Enter or Space.
- Make instructions, validation errors, and status messages understandable without relying only on color or visual position.
- Consider the clarity of messages produced by the Java/Spring Boot backend when they appear in the user interface.
- Update affected documentation and describe any remaining barrier or confirmed workaround in the pull request (PR).

For user-facing changes, please include a short accessibility note in the PR description covering:

1. **Scope:** Which pages or tasks changed, and whether they belong to the Legacy UI, Modern UI, or both.
2. **Checks performed:** For example, keyboard use or readability checks. Name any assistive technology and browser actually used.
3. **Results and gaps:** What worked, what did not, and what was not tested.
4. **Optional evidence:** Screenshots or recordings when useful, without exposing sensitive or personal information.

The project's [contribution guidelines](CONTRIBUTING.md) describe Java formatting and code-quality checks using Spotless and Checkstyle, including CI checks. These are **not accessibility tests**. This document does not assume that a particular automated accessibility tool or accessibility CI check is already in place.

## Reporting accessibility issues

If something is hard to see, understand, navigate, or use, or you need help with an accessibility barrier, you can:

- [Open a GitHub Issue](https://github.com/SasanLabs/VulnerableApp/issues/new/choose).
- Email [karan.sasan@owasp.org](mailto:karan.sasan@owasp.org), the contact listed in the project's contribution guidelines, if GitHub is difficult to use or you prefer email.

Please share as much of the following as is practical:

- The task you were trying to complete and the steps that led to the barrier.
- The page URL and whether you were using the Legacy UI or Modern UI.
- What happened and what you expected to happen.
- The project version, browser, operating system, and device, if known.
- Any relevant input method or assistive technology, such as keyboard navigation or a screen reader, including versions if helpful.

**Screenshots and recordings are optional. You do not need to disclose a disability.** Please do not include real passwords, tokens, or other sensitive information in a public report. For security-related reporting and safe use of this deliberately vulnerable application, see [SECURITY.md](../SECURITY.md).

### Severity

There is no confirmed project-specific accessibility severity scale described in this statement. The following examples explain impact on a person's ability to complete a task; they are **not official labels or resolution deadlines**:

- **Task blocked:** A key action cannot be completed using a keyboard, and no workable alternative is available.
- **Task significantly difficult:** A screen reader cannot identify an important control, making it difficult to know what action it performs.
- **Task possible with difficulty:** Low-contrast instructions or unclear focus indication make a task slower or harder, even though another usable path exists.

Maintainers can assess and confirm priority during issue triage. Reporters do not need to assign a severity level. If formal severity labels are introduced, they should align with the project's issue templates and handling process.

### How we respond

After a report is submitted, maintainers can review it as capacity allows. Depending on the issue, follow-up may include:

- Acknowledging the report or asking for steps and environment details needed to reproduce it.
- Posting updates about investigation, progress, or reasons a fix remains pending.
- Sharing a workaround when one has been checked and is available.
- Describing a fix and inviting the reporter to verify it, if they are able and willing.

**There is no guaranteed acknowledgement or resolution timeframe for accessibility issues.** The response guidance for PRs in [CONTRIBUTING.md](CONTRIBUTING.md) should not be treated as a deadline for GitHub Issues. If a report has had no update for some time, you may follow up in the original issue or use the email contact above.

## Ownership and maintenance

Accessibility reports and suggestions can be directed to the VulnerableApp maintainers through the existing GitHub Issues and contact channels. No separately designated accessibility owner or fixed review schedule is identified here.

This statement should be reviewed when the user interfaces, contribution process, or reporting channels change materially, and during relevant documentation updates. Maintainers reviewing those changes should check that the guidance and links remain accurate. If responsibility moves to another maintainer, the handover should include open accessibility issues, available test results, known workarounds, and any documentation that needs updating. The team may establish a more specific review schedule or owner in the future.

## Supported environments

VulnerableApp provides two browser-based interfaces, as described in the [README](../README.md):

- **Modern UI:** Provided through VulnerableApp-facade and available when running the application's Docker Compose setup; the README describes local access at `http://localhost`.
- **Legacy UI:** Available when running the standalone application; the README describes local access at `http://localhost:9090/VulnerableApp`.

The README describes how to run the interfaces, but it does not provide a verified accessibility compatibility matrix. The following are **environments to consider for future testing, not a list of tested or guaranteed combinations**:

- **Platforms and devices:** Windows, macOS, Linux, and desktop or mobile devices.
- **Browsers:** Chrome, Firefox, Edge, Safari, and relevant versions.
- **Input methods:** Keyboard, mouse, touch, and voice input.
- **Assistive technologies:** Screen readers, magnification tools, and relevant versions.

Support can differ between the two interfaces and between device and assistive-technology combinations. Confirmed test results should record the interface, versions, test date, method, and any limitations.

## Known limitations

No complete accessibility assessment is available.

In the Legacy UI, the level selector requires a pointer. Keyboard users cannot focus a level or select it with Enter or Space.

Areas that still need documented checks include:

- Whether important tasks can be completed using only a keyboard.
- Whether important controls and form fields have understandable names for screen reader users.
- Whether instructions, feedback, and focus indicators can be seen and understood.
- Whether the same task presents different barriers in the Legacy UI and Modern UI.

As specific barriers are confirmed, they should be described in terms of the affected task and linked to their [GitHub Issues](https://github.com/SasanLabs/VulnerableApp/issues). Where available, include a tested workaround or an equivalent way to complete the task. Do not assume a workaround works without checking it.

## Feedback and improvements

Suggestions about this statement, documentation, user interfaces, or future accessibility checks are welcome through [GitHub Issues](https://github.com/SasanLabs/VulnerableApp/issues) or [karan.sasan@owasp.org](mailto:karan.sasan@owasp.org).

If you are currently experiencing an accessibility barrier, please use the **Reporting accessibility issues** process above so that the problem can be understood and tracked. This statement can be updated as test results, project practices, and community feedback become available.
