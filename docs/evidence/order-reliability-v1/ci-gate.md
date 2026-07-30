# CI gate: Order reliability on MySQL 8.4

- **Job:** `order-reliability` — displayed in GitHub Actions as **Order reliability on MySQL 8.4**.
- **Database:** a GitHub Actions MySQL 8.4 service with a fresh `order_reliability_ci` database for each workflow run. Flyway is applied by the Spring Boot test context.
- **Enablement:** the job sets `ORDER_RELIABILITY_MYSQL_ENABLED=true` and runs `mvn -Dtest=OrderReliabilityMySqlTests test`; therefore the MySQL-only class is executed rather than skipped.
- **Scope:** CI runs one isolated verification pass. The local PowerShell evidence runner retains the stricter three-consecutive-run stability check.
- **AI boundary:** no AI provider configuration is read, no external model is called, and no real secret is used. The order test disables AI rate limiting for its own Spring context.
- **Failure behavior:** Maven receives the test exit code directly, so a scenario assertion, Flyway failure, or MySQL connection failure fails the job.
