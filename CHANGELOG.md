# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased] - 2025-10-15

- fix(OpenAI): Ensure embedding generation variable scoping and add no-arg constructor for test-friendly instantiation.
- chore(Metrics): Instrument OpenAI client with Micrometer timers.
- feat(Scheduler, Email): Add scheduled fine-creation flow and email retry + counters.
- ci: Add GitHub Actions workflow to run Maven tests on push and pull requests.
