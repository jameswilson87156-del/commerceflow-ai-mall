# P3 Issues and Fixes

## Test Helper Arity

### Observation

The first expanded Java test compile failed because the Flyway assertion called a helper that only accepted a parameterized SQL statement.

### Root Cause

The helper overload for a no-parameter count query had not been added.

### Fix and Regression

Added the no-parameter overload and reran the full Maven suite successfully.

## MyBatis XML Registration

### Observation

The first MyBatis test created the mapper proxy but failed with `Invalid bound statement` for the XML method.

### Root Cause

The starter's automatic mapper-resource registration did not load the XML statement under this Spring Boot/MyBatis version combination.

### Fix and Regression

Added `OrderEvidenceMybatisConfig` with an explicit DataSource, mapper XML resource pattern, and mapper scan. The direct mapper and HTTP evidence tests then passed.

## Browser Script Chinese Locator

### Observation

The first Playwright capture script could not find the Chinese order-navigation label.

### Root Cause

The temporary PowerShell pipeline changed the non-ASCII literal before the browser evaluated it; the page text and navigation were intact.

### Fix and Regression

Located the existing second sidebar button structurally, then completed the capture with zero console and page errors. No UI test-only element was added.

## Frontend Command Working Directory

### Observation

One final regression attempt ran `npm` from the repository root, where no `package.json` exists.

### Root Cause

The frontend package is intentionally scoped to `apps/admin-web`, while the Maven Wrapper is intentionally scoped from the repository root.

### Fix and Regression

Ran Vitest and the Vue production build from `apps/admin-web`, then ran Maven from the repository root. Both suites passed; no source change was needed for this command-location issue.

## MySQL EXPLAIN Command Quoting

### Observation

The first read-only MySQL `EXPLAIN` attempt used a nested Docker shell string and failed before MySQL received the query.

### Root Cause

The nested shell quote boundaries did not survive the PowerShell command construction.

### Fix and Regression

Passed MySQL arguments directly through `docker compose exec`. The completed `EXPLAIN` showed the expected `orders.order_no`, OrderItem foreign-key, and inventory-movement unique indexes. No data or schema was changed by the failed command.

## Legacy Test Order Number Length

### Observation

The new legacy-null-snapshot test initially failed while inserting its manual order fixture.

### Root Cause

The generated order number exceeded the existing `orders.order_no VARCHAR(40)` boundary.

### Fix and Regression

The fixture was changed to a valid 38-character value. The schema remained unchanged and the full 14-test Maven suite passed.

## Local Runtime Port Occupancy

### Observation

Ports 8080 and 8081 were already occupied when P3.1 required an isolated API runtime for the screenshot.

### Root Cause

Existing local processes were already using those ports.

### Fix and Regression

P3.1 ran on 8082 and the existing processes were left untouched. The browser verification used an isolated request-forwarding route only for its capture session; it received real responses from the new MySQL-backed API. No project source configuration was changed.
