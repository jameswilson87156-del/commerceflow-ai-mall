# P7C MySQL CI Evidence

The Java workflow starts MySQL 8.4 with a fresh `commerceflow_ci` database and Redis 8.0.2. It runs `mvn test`, then starts the application with CI service environment variables and waits for readiness.

The smoke requests `GET /api/operations/overview` and requires product count `2`, SKU count `5`, created order count `0`, created order amount `0`, and AI interaction count `0`. It also requires the application log to show Flyway applied eight migrations or report the schema up to date. The background Java PID is stopped through a shell trap, and the log is printed only on failure.

P7C also executed the equivalent smoke locally with an isolated Compose project, MySQL mapped to `3310`, Redis mapped to `6381`, and Java mapped to `8082`. The fresh database returned product count `2`, SKU count `5`, created order count `0`, created order amount `0.00`, AI interaction count `0`, and eight successful Flyway version rows. The isolated listener, containers, network, and volume were removed after verification. P7C does not pretend that H2 tests prove this smoke.
