# Showcase V1 Release Checklist (historical)

> This is the historical 2026-07-29 release snapshot. It is retained as release evidence; the current implementation and test counts are maintained in [当前验收矩阵](../evidence/ACCEPTANCE_MATRIX.md) and [当前本地验证结果](../evidence/VERIFICATION_RESULTS.md).

## Release Gates

- [x] P7C final branch backed up to `origin/feat/p7-ci-readme-architecture`.
- [x] Release branch starts from `3736a858516430e0402dac853b4b98a8d28af7ad`.
- [x] Current-tree security and repository-integrity checks pass.
- [x] Reachable-history audit found no real credential or sensitive runtime artifact.
- [x] Historical local-path wording has been removed from the current release tree without rewriting history.
- [x] License and asset provenance records confirm comparison-only open-source research and original Showcase product assets.
- [x] Full local regression and clean MySQL/Flyway smoke pass.
- [ ] Release branch is pushed without force.
- [ ] `main` is created at the verified P6 final ancestor and set as the default branch.
- [ ] Pull request `release/showcase-v1.0.0` to `main` is created.
- [ ] All five GitHub Actions jobs pass.
- [ ] Pull request is merged with a merge commit.
- [ ] Annotated tag `showcase-v1.0.0` points to merged `main`.
- [ ] Published GitHub Release uses `SHOWCASE_V1_RELEASE_NOTES.md`.
- [ ] Repository remains Private and all historical branches remain retained.

## Local Evidence Targets

- Java complete suite: at least 45 tests, including five Redis integration tests.
- Python complete suite: at least 11 tests.
- Admin complete suite: at least 45 tests, type check, and production build.
- Mobile complete suite: at least 31 tests, H5 build, and UniApp H5 compilation.
- Repository-integrity, README/architecture links, and canonical screenshot dimensions.
- Isolated MySQL/Flyway overview smoke with the expected Showcase dataset.

## Completed Local Validation

| Gate | Result |
| --- | --- |
| Repository integrity and history audit | PASS |
| Java suite | 45 passed, including 5 Redis integration tests |
| Python suite | 11 passed |
| Admin suite | 45 passed; type check and production build passed |
| Mobile suite | 31 passed; H5 and UniApp H5 builds passed |
| Clean MySQL/Flyway smoke | PASS: Flyway V1-V8, 2 products, 5 SKUs, 0 orders, amount `0.00`, 0 AI interactions |
| PowerShell parsing | PASS for all `scripts/**/*.ps1` files |

## Scope Guard

This release permits only documentation, release validation, integrity checks, and CI-release fixes. It does not change orders, inventory, idempotency, AI answers, Evidence, Trace, Redis Lua, rate limiting, mobile business flows, Flyway schemas, or formal screenshot content.
