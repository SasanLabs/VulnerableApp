# Technical Design Documentation

While designing VulnerableApp, Major emphasis was given on **Ease of adding Vulnerabilities** such that developers of Vulnerability Scanners need not to work a lot
to add a vulnerable code.

1. Ease of Adding Vulnerabilities
2. Less learning curve for adding vulnerabilities as it is quite same as Spring boot annotation design
3. Rework


# Internal Design
For adding a new Vulnerability following are the steps which needs to be done:
1. Annotations
2. Adding Html Pages


# Annotations:
**VulnerableAppRestController** is the annotation which exposes the vulnerability as a Rest Endpoint. This annotation is annotated with Spring's meta annotation **RestController** which is used to expose the Rest Endpoints.
Information added to this annotation will be visible in the UI and helps the users to understand about the vulnerability.

