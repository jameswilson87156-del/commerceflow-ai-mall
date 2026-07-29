# CommerceFlow AI Mall Showcase v1.0.0

## Positioning

CommerceFlow AI Mall is a local, interview-oriented Commerce and AI customer-service Showcase. It demonstrates a Java modular monolith, a Python FastAPI AI boundary, Vue admin workflows, and a UniApp H5 mobile flow using reproducible local demo data.

## Core Capabilities

- Product, SKU, and available-inventory browsing with local Showcase assets.
- Cart and order submission with `BigDecimal` amounts, order-item snapshots, MySQL transactional stock deduction, idempotency-key handling, and inventory-movement evidence.
- Vue admin pages for product/SKU, order execution evidence, AI customer-service evidence/trace, and operations overview.
- UniApp H5 product, cart, order, and AI customer-service flows using the local API.
- Deterministic `commerceflow-mock` AI mode with structured business facts, evidence, trace, fallback boundaries, and no real API key requirement.
- Redis-backed fixed-window protection for the AI customer-service endpoint, including `429`, `Retry-After`, a unified cooldown UI, and documented FAIL_OPEN behavior.

## Validation

The release branch includes local suites for Java, Redis integration, Python, Vue admin, and UniApp mobile, plus production builds, repository-integrity checks, canonical screenshot validation, and an isolated MySQL/Flyway smoke test. GitHub Actions runs the same repository-integrity, Java backend, Python AI service, admin web, and mobile app gates for the release pull request.

## Run Locally

Use the repository onboarding documents and scripts:

- `README_FIRST_RUN.md`
- `TROUBLESHOOTING.md`
- `scripts/showcase/start.ps1`
- `scripts/showcase/verify.ps1`
- `scripts/showcase/stop.ps1`

The normal Showcase uses local MySQL, Redis, Java, Python, and frontend services. It uses no external AI provider and no real API key.

## Known Boundaries

- This is a local Showcase, not a production system or a real merchant deployment.
- Only the `CREATED` order state is implemented; payment, logistics, refunds, addresses, coupons, and real user authentication are out of scope.
- Redis protects only the AI customer-service endpoint. It does not participate in orders, inventory, or MySQL transactions.
- The rate-limit identity key is not production authentication and the limit does not claim DDoS protection.
- AI responses are deterministic mock/fallback suggestions based on Java-provided business facts; the Python service does not write the mall database or invent stock facts.
- Product images are approved original Showcase assets; generated design references are not runtime evidence.
