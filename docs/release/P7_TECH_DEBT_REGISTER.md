# P7 Technical Debt Register

| ID | Priority | Debt / boundary | Why it matters | Planned resolution |
| --- | --- | --- | --- | --- |
| TD-01 | P1 | No coordinated local lifecycle | Four services and changing ports make a reviewer setup fragile. | P7B plan and implementation. |
| TD-02 | P1 | CORS and H5 port configuration are not unified in the default path | H5 has previously required an explicit override for a non-default port. | P7B configuration contract and smoke check. |
| TD-03 | P1 | CI is build-heavy, not release-complete | It lacks MySQL/Flyway, front-end tests, mobile tests, secret scan, and asset/screenshot validation. | P7C. |
| TD-04 | P1 | README is V1-era and short | A public reviewer cannot reliably learn scope, commands, evidence, and boundaries. | P7C. |
| TD-05 | P1 | Remote default branch remains historic P3 and no `main` exists | Release governance is unclear. | P7D after successful candidate review. |
| TD-06 | P2 | JDBC is used for domain persistence while MyBatis is limited to order evidence | The boundary is deliberate, but unfamiliar readers need an explanation. | Architecture docs now; do not migrate tools merely for uniformity. |
| TD-07 | P2 | Demo identity defaults are distributed | Fine for local Showcase, not a future authentication design. | Defer until an auth phase. |
| TD-08 | P2 | Fixed-window Redis policy and FAIL_OPEN are showcase choices | Neither is DDoS protection nor production authentication. | Keep documented; re-evaluate with a real deployment threat model. |
| TD-09 | Accepted boundary | Local MySQL data, deterministic AI, and seed inventory are not merchant data | Avoids fabricated commercial claims. | Keep as Showcase seed. |
| TD-10 | Accepted boundary | Payment, logistics, refunds, addresses, coupons, reporting analytics are absent | Scope is intentionally focused. | Do not backfill for release optics. |

P0 status: no P0 item was discovered. A P0 is reserved for a reproducible corruption, exposure, safety, or release-blocking defect found during P7D final verification.
