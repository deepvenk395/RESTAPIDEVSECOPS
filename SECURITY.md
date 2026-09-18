# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

This project is a **practice target**. Nevertheless, please report any vulnerability privately
by opening a draft GitHub Security Advisory (Settings > Security > Security Advisories) or by
contacting the repository owner directly. Do **not** open a public issue for security findings.

## What happens next

1. The report is acknowledged within 5 business days.
2. A fix + regression test is produced.
3. A release note is published.

## Security expectations

- All changes go through CI/Security pipelines (automated secret scan, dependency
  review, SAST, SCA and container scanning) before merge.
- Secrets are never committed to the repository.
- The default JWT secret is dev-only and the application refuses to start with it in the `prod` profile.