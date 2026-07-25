# CommerceFlow AI Mall

E-commerce and AI customer-support collaboration platform for AI application, Java full-stack, Java backend, and Python AI internship applications.

> Showcase V1 is intentionally honest: Codex contributed substantially to the showcase implementation. Ownership verification is tracked in `docs/career/OWNERSHIP_GAPS.md`; a later `commerceflow-learning-rebuild` will be written by the learner.

## Stack

- Java 17 + Spring Boot 3 modular monolith
- MySQL 8.4 + Flyway
- Vue 3 + TypeScript admin console
- UniApp user flow
- Python 3.12 + FastAPI AI side service
- Mock Provider by default; optional OpenAI-compatible provider

## Quick Start

See [README_FIRST_RUN.md](README_FIRST_RUN.md). The shortest path is:

```powershell
Copy-Item .env.example .env
./scripts/start-mysql.ps1
./mvnw.cmd -f apps/mall-api/pom.xml test
./mvnw.cmd -f apps/mall-api/pom.xml spring-boot:run
```

Then open `http://localhost:8080/swagger-ui.html` and run the demo login, product, cart, order, and AI flows.

## Current Status

- PROJECT_DIRECTION_STATUS: APPROVED
- SHOWCASE_IMPLEMENTATION_STATUS: APPROVED
- OWNERSHIP_VERIFICATION_STATUS: NOT_PASSED
- LEARNING_REBUILD_STATUS: NOT_STARTED

This is a job-search showcase, not a production system and not a claim of independent from-scratch authorship.
