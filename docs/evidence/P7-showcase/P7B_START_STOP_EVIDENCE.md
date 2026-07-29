# P7B Start and Stop Evidence

`start.ps1` was exercised with Compose project `commerceflow-showcase`. MySQL and Redis reported healthy before Java, FastAPI, Admin, and Mobile H5 were started. The verified services were Java `8081`, Python `8000`, Admin `5174`, and Mobile `5173`.

`status.ps1` then reported API aggregate health and readiness separately, Python provider mode, Admin/Mobile HTTP status, Redis rate-limit policy, Compose health, Flyway version, URLs, branch, and worktree state.

`verify.ps1 -IncludeMobile` passed. It checked readiness, Python health, Mock provider mode, product API, cart/order reads, overview API, the five local image responses, one normal Mock AI response with evidence and trace, and all rate-limit response headers.

The final close-out exercised `stop.ps1`: it stopped only recorded, identity-matching Showcase child processes and the `commerceflow-showcase` MySQL/Redis services. The unrelated listener on port `8080` was preserved. Its default path did not remove Docker data; `-RemoveData` requires the literal confirmation `REMOVE_DATA`.
