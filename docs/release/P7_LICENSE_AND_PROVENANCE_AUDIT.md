# P7 License And Provenance Audit

## Recorded Sources

`THIRD_PARTY_NOTICES.md` and `docs/reference/SOURCE_PROVENANCE.md` record mall, CRMEB, litemall, and newbee-mall references as comparison-only. Their licenses are recorded there, including Apache-2.0, MIT, and GPL-3.0 comparison references. The current record explicitly says no external source, SQL, template, UI, seed data, screenshot, logo, icon, or product image was copied.

P4, P5, and P6 provenance files document official framework/reference research and declare independent implementation. Vue, Spring Boot, FastAPI, UniApp, Redis, MySQL, and their package/image licenses remain dependency notices rather than copied application code.

## Assets

- `docs/design_refs/source/product-sku/target-page.png` is an AI-generated design reference, not runtime proof.
- The approved T-shirt and tote images are original project Showcase assets with no third-party brand, logo, or watermark claim; app copies are local runtime resources.
- Real screenshots are separate runtime evidence under `screenshots/v2`.

## Public Suitability Decision

The current written provenance is sufficient to proceed to P7D public-readiness review, but it is not a legal opinion. Before public release, verify every new dependency/asset added after this audit, preserve notices, and record any actual code reuse with exact source, license, range, and approval. Do not claim independent provenance beyond the repository records.
