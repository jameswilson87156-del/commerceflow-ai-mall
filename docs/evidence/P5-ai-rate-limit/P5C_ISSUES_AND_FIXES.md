# P5C Issues And Fixes

| Observation | Root cause | Fix / outcome |
| --- | --- | --- |
| P5B PNG had a 408px effective sidebar despite CSS width 272px. | The in-app capture surface remained at host DPR 1.5 while synthetic metrics reported 1920x1080. | Used a true Chrome 1920x1080 DPR 1 viewport for P5C evidence. |
| Initial readiness test returned 404. | The test `application.yml` shadows the main configuration and did not include the new health group. | Added the same readiness group to test configuration; the test now passes. |
| Initial 429 prefill did not throttle the page request. | Preload used `127.0.0.1` while the page called `localhost`; the remote-address HMAC inputs differed. | Re-ran through the page API host and confirmed five 200 responses followed by a real UI 429. |
| Redis restart initially left aggregate health at 503. | Lettuce needed a short reconnect interval after the container became healthy. | Waited for the real indicator to recover, then verified Redis mode returned without Java restart. |
| Repeated chat answers extended the page. | The middle message list did not own a constrained grid area. | Added viewport-relative panel height, `minmax(0, 1fr)`, `min-height: 0`, and internal overflow. |

No issue was found in the existing Lua atomicity, HMAC key privacy, request validation order, no-trace-on-429 rule, or `FAIL_OPEN` no-fake-quota rule.
