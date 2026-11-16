# Security Policy

VulnerableApp is a deliberately vulnerable application built for learning,
for practicing how to find and exploit common vulnerabilities in a safe environment, and for testing security tools.
Many of its weaknesses are intentional, so this policy explains what to report and what not to.

For more information, see [Documentation and References](README.md#documentation-and-references).

## Supported Versions ✅

> [!NOTE]
> Only the latest release is maintained.

Please make sure you are using the [latest release](https://github.com/SasanLabs/VulnerableApp/releases/latest) before reporting a problem.


## Reporting a Vulnerability

### Intentional vulnerabilities ⚠️

> [!IMPORTANT]
> The vulnerabilities in the application's vulnerable endpoints and levels are by design. 
If a scanner or a manual test finds them, **please do not report them**. They are the purpose of the project.

Check out all the vulnerabilities here [Currently handled Vulnerability types](README.md#currently-handled-vulnerability-types)

### Unintended problems 🐞

If you find a genuine, unintended problem, please report it. How you report it depends on how serious it is.

**Exploitable security issues:** please **do open a public issue**, with the details, and we will try our best to help you.

**Everything else** (non-sensitive bugs, build problems, hardening suggestions): please
[open a new issue](https://github.com/SasanLabs/VulnerableApp/issues/new/choose).
Don't know how to report a bug? Read [bug_report.md](.github/ISSUE_TEMPLATE/bug_report.md).

This covers problems in:

- Build or CI configuration
- Docker images
- Dependencies
- The benchmark endpoint

Please include the version or image tag, the steps to reproduce, and what you expected to happen.

### Running VulnerableApp safely

> [!WARNING]
> Because the application is vulnerable on purpose,
> run it only in isolated environments (for example, an isolated Docker container or virtual machine). **Never expose it to the internet.**

Things to keep in mind:
- Run it only on your own machine, in a virtual machine, or in a Docker container.
- Do not deploy it on a public server.
- Do not expose it to the internet, you can do it, but you need to be really careful with that.
- Do not run it on a machine that holds personal data, credentials, or production access.
- Do not reuse real passwords, API keys, or tokens while testing it, you can do it only if testing is done locally.
- Practice only against your own running instance, never against someone else's deployment.
- Stop and remove the container or VM when you are done.

