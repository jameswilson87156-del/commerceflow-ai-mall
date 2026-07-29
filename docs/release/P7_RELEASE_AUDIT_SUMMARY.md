# P7 Release Audit Summary

1. **Completeness:** P2-P6 form a coherent local Showcase: catalog, cart/order evidence, grounded AI, Redis limiter, admin, and mobile H5.
2. **Real issues:** README, coordinated startup, CI coverage, Git default-branch governance, and public-history security review remain.
3. **Required fixes:** No P0 found. P1 work is P7B runtime/CORS and P7C CI/README, followed by P7D security/reproducibility.
4. **Accepted debt:** Mock AI, demo identity/data, fixed window/FAIL_OPEN, H5-only runtime acceptance, JDBC/MyBatis split, and excluded commerce modules.
5. **Operations overview:** Build it only in P7B as a truthful, read-only local-data view; no fake KPI or analytics migration.
6. **README gap:** Current README is accurate but V1-level; P7C must use the 20-item plan.
7. **CI gap:** Existing CI has Java/Python/build jobs; it lacks MySQL/Flyway, UI tests, mobile tests, secret and evidence checks.
8. **Startup gap:** Helpers exist but not one coordinated safe lifecycle; P7B plans it.
9. **Canonical screenshots:** Four desktop proofs maximum: P2 product, P3 order, P4 AI, P5 429; mobile uses separate grouped evidence.
10. **Public readiness:** Conditional only; P7D must scan full history and re-verify locally before visibility change.
11. **Main and tag:** Neither exists yet. Establish through reviewed P7D candidate, then tag `showcase-v1.0.0`.
12. **Final ordering:** P7B -> P7C -> P7D -> P7E.
13. **Commit count:** Decide during P7D; prefer focused commits by phase rather than a target number.
14. **P7A changes:** Documentation-only audit and plan, with no remote mutation.
15. **Next task:** P7B should first lock the safe runtime port/CORS contract and decide whether every operations-overview metric can be served by current real data.
