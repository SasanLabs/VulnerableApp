# Email flow testing

## Overview

VulnerableApp includes a simple email testing endpoint for validating the mail integration flow. The endpoint is available under the unsafe profile and sends a test message through the configured mail service.

## Endpoint

Send a test email with:

```bash
curl -X POST "http://localhost/VulnerableApp/email/test" \
  -d "to=you@example.com" \
  -d "subject=Test email" \
  -d "body=Hello from VulnerableApp" \
  -d "html=false"
```

You can also trigger the reset and verification flows exposed by the same controller:

```bash
curl -X POST "http://localhost/VulnerableApp/email/reset" \
  -d "to=you@example.com" \
  -d "token=reset-token"

curl -X POST "http://localhost/VulnerableApp/email/verify" \
  -d "to=you@example.com" \
  -d "token=verify-token"
```

### Parameters

- `to` (required): recipient email address
- `subject` (optional): email subject; defaults to `VulnerableApp test email`
- `body` (optional): email body; defaults to `This email was sent by VulnerableApp.`
- `html` (optional): set to `true` for HTML email content; defaults to `false`

### Reset and verification endpoints

- `/email/reset`: sends a password reset email using the provided `to` address and `token`
- `/email/verify`: sends an email verification message using the provided `to` address and `token`
- `token` (required): appended to the reset or verification URL and URL-encoded before sending

## Mailpit / SMTP setup

The application uses the standard Spring mail configuration.

### Default properties

The shared application properties define the default SMTP host and credentials behavior:

- `spring.mail.host=${SMTP_HOST:mailpit}`
- `spring.mail.port=${SMTP_PORT:1025}`
- `spring.mail.username=${SMTP_USERNAME:smtp_hacker}`
- `spring.mail.password=${SMTP_PASSWORD:smtp_password}`
- `vulnerableapp.email.from=${EMAIL_FROM:no-reply@vulnerableapp.local}`
- `vulnerableapp.email.base-url=${APP_BASE_URL:http://localhost}`

If Mailpit is running locally, emails will be captured there and available at:

- `http://localhost:8025`

## Notes

- If the mail server is unavailable, the application logs a warning and continues running instead of failing the request.
- The email testing endpoint is intended for local validation and debugging.
