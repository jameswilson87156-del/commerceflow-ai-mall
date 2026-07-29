# P7B CORS Evidence

The CORS allowlist moved from Java source defaults to `commerceflow.cors.allowed-origins` in `application.yml`. The template resolves the Admin and Mobile H5 origins from environment values and does not use `*`.

`CorsConfigurationTests` verifies a configured allowed origin and checks exposure of `Retry-After`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, and `X-RateLimit-Mode`. It also verifies that an unrelated malicious origin does not receive `Access-Control-Allow-Origin`.

No CORS policy accepts arbitrary origins, and no private host, password, API key, or Redis configuration is returned by the operations overview.
