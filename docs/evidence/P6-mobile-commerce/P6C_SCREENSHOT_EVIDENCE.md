# P6C Screenshot Evidence

All screenshots are real local H5 pages at 390x844, DPR 1, zh-CN, zoom 100%, without a reference-image background, cropping, stretching, or stitching.

| State | File | Real evidence |
| --- | --- | --- |
| Normal | `screenshots/v2/07-mobile-ai-customer-service-real.png` | SKU 10004 gray L, stock 24, local Mock answer, Redis quota, Evidence/Trace entries |
| 429 | `screenshots/v2/07-mobile-ai-customer-service-429-real.png` | real sixth 429, limit 5, remaining 0, one disabled countdown |
| Fallback | `screenshots/v2/07-mobile-ai-customer-service-fallback-real.png` | Python stopped, Java fact fallback, FALLBACK provider |
| FAIL_OPEN | `screenshots/v2/07-mobile-ai-customer-service-fail-open-real.png` | Redis stopped, degraded disclosure, no quota fabrication |

Historical P6B screenshots remain unchanged.
