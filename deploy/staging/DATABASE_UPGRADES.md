# Staging database contract

This is the new empty-database path. It must not be pointed at an existing Demo,
staging or production database without a separately reviewed adoption plan.

- Staging/production select `classpath:db/staging-migration` and the independent
  `commerceflow_schema_history` Flyway table. Automatic baseline and clean are
  disabled. The existing `db/migration/V1..V11` files remain unchanged for local
  Demo/test use and historical checksums.
- The new V1 contains schema DDL only: no Demo accounts, products, orders or AI
  records. Empty catalog is expected until independently approved test data is
  provisioned. Do not map a real user to the old Demo account id=1.
- The application login uses `COMMERCEFLOW_DB_USERNAME/PASSWORD`; Flyway uses
  separate `COMMERCEFLOW_DB_MIGRATION_USERNAME/PASSWORD`, restricted to this
  project's database. Supply both through this project's runtime Secret store.
- The Compose file connects to existing private RDS/Tair and creates neither a
  database nor a Redis service. Set JDBC `sslMode=VERIFY_IDENTITY`; provide the
  RDS CA in the JVM trust store where required. Tair TLS must match the selected
  endpoint/port and JVM trust configuration. Do not disable verification to make
  an unverified cloud endpoint start.

Before first cloud start: confirm the database is empty, resource ownership,
private connectivity, CA trust and backup policy. Starting the API would execute
DDL, so it is a separate authorized deployment step, not a config check.

For each later change, add V2/V3 to the new stream without editing an applied
file; record the checksum, schema version, image digest and backup reference.
Restore a backup into a temporary database and rehearse the migration there.
MySQL DDL is not an application transaction: on partial failure stop the release
and investigate; never automatically clean, repair, baseline or rerun destructive
SQL. Only roll back the application when the previous version supports the new
schema. Otherwise preserve the source database and recover to a new database
through a reviewed recovery plan.

The local MySQL and Flyway checks prove the new empty-schema path only. Existing
database adoption, cloud restoration, CA trust and actual RDS/Tair connectivity
remain STAGING_PENDING.
