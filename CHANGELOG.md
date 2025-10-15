# Changelog

All notable changes to this project will be documented in this file.

## Unreleased
- Add Micrometer repository-backed Gauges: `outstanding.fines.total`, `currently.borrowed.count`.
- Implement idempotent fines scheduler and notification flow.
- Add Email Outbox pattern (durable email sending) with `EmailOutbox` entity and scheduled `OutboxSender` worker.
- Update CI to add integration job that runs integration tests with `-Dspring.profiles.active=test` so actuator metrics are available during that job.
# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased] - 2025-10-15

- fix(OpenAI): Ensure embedding generation variable scoping and add no-arg constructor for test-friendly instantiation.
- chore(Metrics): Instrument OpenAI client with Micrometer timers.
- feat(Scheduler, Email): Add scheduled fine-creation flow and email retry + counters.
- ci: Add GitHub Actions workflow to run Maven tests on push and pull requests.
