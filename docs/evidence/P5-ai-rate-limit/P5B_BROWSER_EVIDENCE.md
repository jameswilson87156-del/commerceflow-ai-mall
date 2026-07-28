# P5B Browser Evidence

Final local process set:

- MySQL Docker Compose on 3307, reset from an empty volume.
- Redis Docker Compose on 6380.
- Python FastAPI on 8000 with `commerceflow-mock`, no external model call and no real API key.
- Java API on 8081 with Flyway V1-V8.
- Vue Vite app on 5173.

Captures use `zh-CN`, exact 1920x1080 CSS viewport, device scale factor 1, and CDP PNG capture without cropping or reference-image embedding.

| State | Result |
| --- | --- |
| Normal | Redis card shows limit 5 / remaining 4; local mock answer, Java Evidence, and Trace are present. |
| 429 | Retained question, countdown, disabled submit, no sixth answer. |
| Recovery | At the server-provided boundary, submit re-enabled and the question value remained. |
| FAIL_OPEN | Redis stop shows degraded disclosure; restore returns normal Redis mode. |
| Console | 0 error-level messages in each screenshot state. |
| Assets | Cache-cleared local product PNG requests all returned 200. |
