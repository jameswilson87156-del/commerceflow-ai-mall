# P7B Issues and Fixes

| Observed issue | Root cause | Resolution |
| --- | --- | --- |
| Overview mapper was not resolved | Mapper scan covered only prior order mapper package | Added `OperationsOverviewMapper` to the existing MyBatis scan without changing JDBC writes |
| MyBatis record construction failed | JDBC mapped numeric aggregate values as boxed values while record constructors had incompatible primitive mapping metadata | Used boxed record row fields with explicit XML constructor mappings |
| Admin build failed during first iteration | `loadEnv` was imported from the wrong package | Imported it from Vite; Admin test/build then passed |
| Navigation type became `never` | Inferred union did not preserve all views | Declared the `View` union and navigation item shape explicitly |
| Page used a GMV-like label | Showcase order sum could be misunderstood as commercial revenue | Labelled it local Showcase order amount and explicitly marked it non-commercial revenue |
| PowerShell start parsing failed | Parameter handling and `"${log}.err"` interpolation needed PS 5.1-safe syntax | Used named parameters, braced interpolation, and UTF-8 BOM scripts |
| Script array checks failed on singleton results | PowerShell scalar values do not always expose collection semantics | Wrapped listener/health results with `@(...)` before checking count |
| Default API port was occupied | Unknown WSL relay already listened on `8080` | Failed closed; verified with explicitly overridden `8081` rather than touching the foreign process |
| Wrapper PID differed from listener PID | Maven/npm launch child listeners | Resolve and record the verified listener after readiness |
| Java overview test leaked traces | Spring test context reused `ai_trace` between test methods | Each overview scenario clears only trace data before its own assertion |
| Root npm command failed earlier | Repository root has no `package.json` | Final Admin and Mobile commands ran only from their module directories |
