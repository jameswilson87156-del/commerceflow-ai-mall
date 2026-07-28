# P6B Build Evidence

## Commands

```text
apps/mobile-app/npm test
apps/mobile-app/npm run build
apps/mobile-app/npm run build:uni
apps/admin-web/npm test
apps/admin-web/npm run build
apps/mall-api/./mvnw.cmd -q -f apps/mall-api/pom.xml test
services/ai-service/.venv/Scripts/python.exe -m pytest -q
```

## Results

- Mobile helper tests: 6 passed.
- Mobile Vite H5 build: passed.
- UniApp non-H5 build: `DONE Build complete`.
- Admin Vue tests: 43 passed.
- Admin Vue type check and production build: passed.
- Java: 42 passed, 0 failures, 0 errors.
- Python: 11 passed.

Build output included existing Uni alpha and Vite deprecation notices; no build failure remained.
