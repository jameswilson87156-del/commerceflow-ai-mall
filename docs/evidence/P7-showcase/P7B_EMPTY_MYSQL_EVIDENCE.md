# P7B Empty MySQL Evidence

An isolated Compose project, `commerceflow-p7b-empty`, started a new MySQL 8.4 volume on `3310` and Redis on `6381`. Java started separately on `8082`, executed Flyway V1–V8, and queried the real MySQL overview.

The first migrated response was:

- products `2`, SKUs `5`, available stock `434`, low-stock SKUs `2`;
- created orders `0`, local Showcase order amount `0.00`, recent orders `[]`;
- AI interactions `0`, AI provider `NONE`, AI trace summary `0`.

The nonzero catalog values are required by immutable V2 Showcase seed migration. Therefore a V1–V8 migrated database with a completely zero catalogue is not a supported state and was not falsely claimed.

After a real two-SKU request (SKU `10004` plus `10003`), the isolated API returned one order, two items, amount `328.00`, total available stock `432`, and one recent order. A normal Mock AI request returned provider `commerceflow-mock`, seven evidence records, and six trace steps; the aggregate then reported one AI interaction. The isolated Java listener, Compose containers, network, and volume were removed after this test. The normal local Showcase volume was retained.
