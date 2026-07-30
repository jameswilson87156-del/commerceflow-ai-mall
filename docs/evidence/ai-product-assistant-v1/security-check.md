# Security check

- No provider environment-variable value was read, printed, masked, hashed, logged, or committed.
- Evidence records only configuration source, adapter type, protocol compatibility, and presence-only state.
- The real smoke used a synthetic catalog question and the application chain only.
- Offline tests clear project, shared, and legacy provider environment variables and use local `httpx` transport stubs.
- Provider errors are typed and sanitized; upstream body, authorization material, endpoint, model value, cookie, and request headers are not persisted.
- No `.env` file was created or changed.
- No database migration, order/inventory production code, Admin code, or Mobile code was changed.
- AI1.3 used one synthetic SKU-code question through the application entry point. Evidence records only safe outcome categories and booleans; it records no provider identity, model value, endpoint, credential, authorization material, header, raw request, raw response, or local absolute path.
