AI Library Management System (Skeleton)

This repository contains a scaffold for an AI-powered Library Management System built with Java (Spring Boot).

Quick start (development):

1. Requirements: Java 17+, Maven

2. Build and run:

```powershell
mvn clean package
mvn spring-boot:run
```

3. H2 console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:ailibrary)

4. Seeded users:
- admin / adminpass (ROLE_ADMIN)
- student / studentpass (ROLE_STUDENT)

Notes:
- This is a starting scaffold; AI integrations, payment gateway, voice assistant, and frontend are provided as stubs and extension points.
- Configure SMTP in `src/main/resources/application.properties` for email delivery.

Payment (Stripe) configuration
------------------------------

You can optionally enable Stripe payment integration. The project exposes a few properties under the `payment.stripe` prefix.

- `payment.stripe.enabled` (boolean) — when `true` the `StripePaymentGateway` bean will be created. Defaults to `false`.
- `payment.stripe.webhook-enabled` (boolean) — when `true` the `/webhooks/stripe` endpoint will be registered to receive Stripe events. Defaults to `false`.
- `payment.stripe.webhook-secret` (string) — the Stripe webhook signing secret (the value that starts with `whsec_...`). When set, incoming webhook requests are verified.

For development and tests the webhook controller may be enabled independently from the gateway itself. Webhooks are secured by signature verification; the HTTP endpoint is left open to POSTs so Stripe can deliver events.

Example application.properties
------------------------------

Add the following to `src/main/resources/application.properties` (or to your environment-specific config) to enable Stripe and webhooks in development:

```properties
# Enable the Stripe gateway (false by default)
payment.stripe.enabled=true

# Your Stripe secret key (sk_test_... or sk_live_...)
payment.stripe.secretKey=sk_test_XXXXXXXXXXXXXXXXXXXX

# Currency (default usd)
payment.stripe.currency=usd

# Enable the webhook controller to receive stripe events
payment.stripe.webhook-enabled=true

# The Stripe webhook signing secret (whsec_...)
payment.stripe.webhook-secret=whsec_XXXXXXXXXXXXXXXXXXXX
```

Security note: keep `payment.stripe.secretKey` and `payment.stripe.webhook-secret` out of source control. Use environment variables, a secrets manager, or your CI/CD platform's secret store.

Testing & Metrics
-----------------

The project includes Micrometer metrics and a small set of integration tests that rely on Actuator being accessible during tests. To run integration tests that require actuator metrics, run Maven with the `test` profile active:

```powershell
mvn -Dspring.profiles.active=test -DskipTests=false test
```

Email delivery is implemented using an outbox pattern: the `EmailService` writes outgoing messages to the `email_outbox` table and a scheduled `OutboxSender` worker attempts delivery and records status and attempts. Configure your SMTP settings in `application.properties` for actual email delivery.

