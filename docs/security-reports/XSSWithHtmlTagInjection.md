# Reflected Cross-Site Scripting (XSS) via Unsanitized Query Parameter Reflection

## Summary
A Reflected Cross-Site Scripting (XSS) vulnerability exists in the `XSSWithHtmlTagInjection` controller at `LEVEL_1`. The application directly reflects arbitrary query parameter values into the HTML response without any output encoding or sanitization, allowing attackers to execute arbitrary JavaScript in the context of the victim's browser session.

## Vulnerability Details
- **Vulnerability Type**: Reflected XSS
- **Affected URL**: `/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_1`
- **Affected Parameter**: Any URL query parameter

The controller method `getVulnerablePayloadLevel1` extracts all query parameters and passes them directly to `String.format("<div>%s<div>", map.getValue())`. This formatted string is passed back to the client as an HTTP response. Because Spring Boot sets `Content-Type: text/html` by default for string responses, modern browsers render and execute injected `<script>` tags.

## Steps to Reproduce
1. Navigate to: `http://localhost:9090/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_1?payload=<script>alert('XSS')</script>`
2. Observe the execution of the JavaScript alert.

## Impact
If successfully exploited, an attacker can steal session cookies, hijack user sessions, perform unauthorized actions on behalf of the user, or redirect the user to malicious sites.

## Remediation
Apply strict context-aware output encoding to all user-supplied data before rendering it in the HTML response. In Spring, use `HtmlUtils.htmlEscape()` or `StringEscapeUtils.escapeHtml4()` on the parameter value before appending it to the payload builder.
