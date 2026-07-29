# P7D Git History Security Audit

**Audit date:** 2026-07-29  
**Release branch:** `release/showcase-v1.0.0`  
**Scope:** every reachable local and remote Git ref before the Showcase V1 release.

## Commands And Coverage

- `git rev-list --objects --all`
- `git log --all`
- `git log -p --all`
- `python scripts/ci/audit_git_history.py`
- `python scripts/ci/verify_repository.py`
- Git object-size and tracked-path review

`audit_git_history.py` walks every reachable commit tree, de-duplicates blobs, checks disallowed artifact paths, checks obvious credential patterns, checks non-empty `OPENAI_API_KEY` and `REDIS_PASSWORD` assignments, and records absolute local paths. It is a release gate for this repository; it is not a substitute for a professional credential-scanning service.

## Result

| Check | Result | Notes |
| --- | --- | --- |
| Reachable-history credential patterns | PASS | No GitHub token, API key, private key, bearer token, non-empty OpenAI key, or non-empty Redis password was found. |
| Forbidden historical artifact paths | PASS | No tracked `.env`, database, archive, log, PID, dependency-cache, build-output, or runtime directory was found. |
| Large Git objects | PASS | No reachable Git object exceeds 5 MiB. The largest retained objects are documented Showcase images. |
| Current tracked-text path and LAN-IP review | PASS | The repository-integrity gate rejects local absolute paths and personal-LAN IP addresses in tracked text. |
| Historical absolute paths | REVIEWED | Three pre-release documentation blobs recorded local workspace/research locations. They contain no credential, token, database data, or personal secret. The current release tree replaces those expressions with `<repo-root>` or `<local-research-root>`. No history rewrite was performed. |

## Historical Non-Sensitive Findings

The scan retains these findings because the earlier commits remain reachable and this release does not rewrite history:

| Commit | File | Disposition |
| --- | --- | --- |
| `3736a858516430e0402dac853b4b98a8d28af7ad` | `docs/evidence/P6-mobile-commerce/P6_CURRENT_BASELINE.md` | Current-tree wording changed to `<repo-root>`. |
| `3736a858516430e0402dac853b4b98a8d28af7ad` | `docs/reference/OPEN_SOURCE_LICENSE_MATRIX.md` | Current-tree wording changed to `<local-research-root>`. |
| `3736a858516430e0402dac853b4b98a8d28af7ad` | `docs/reference/SOURCE_PROVENANCE.md` | Current-tree wording changed to `<local-research-root>`. |

## False-Positive Handling

The scan treats an empty Spring placeholder such as `${REDIS_PASSWORD:}` as configuration, not a password. It blocks only an actual non-empty assignment or default. Example variable names in `.env.example` are not treated as real credentials.

## Release Decision

No actual credential was found, so key rotation is not required and the release may continue. If a genuine credential is discovered later, stop release work, revoke the credential, and schedule a separate history-remediation task; do not force-push from this release workflow.

## CI Correction Record

The first release-branch CI run exposed a real configuration mismatch: `AiRateLimitRedisIntegrationTests` intentionally uses the local Showcase Redis port `6380`, while the Actions service had published Redis on `6379`. The workflow now maps container port `6379` to runner port `6380` and passes `REDIS_PORT=6380`. No Java, Redis Lua, rate-limit, or business behavior changed; the correction makes the CI service topology match the tested local contract.

## License And Asset Provenance Cross-Check

`THIRD_PARTY_NOTICES.md`, `docs/reference/SOURCE_PROVENANCE.md`, `docs/release/P7_LICENSE_AND_PROVENANCE_AUDIT.md`, `docs/reference/ASSET_PROVENANCE.md`, and the license matrices were reviewed together. Apache-2.0 and MIT repositories are recorded as comparison-only; GPL repositories are design-and-idea references only. No third-party source code, SQL, template, screenshot, logo, icon, product image, or full reference repository is included. The five runtime product images are approved original Showcase assets, while design-reference images are explicitly non-runtime evidence. This is a repository provenance record, not legal advice.
