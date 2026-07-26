# P4C Issues and Fixes

## Right-Side Response State Did Not Refresh

- **Symptom:** the completed assistant message displayed the response, but the facts/Evidence/Trace panel still showed the selected-SKU preview.
- **Root cause:** the completion path mutated a message object already held in the Vue ref array, so dependent computed state did not receive an array replacement.
- **Fix:** replace the matching message through an immutable `messages.value` array update.
- **Regression evidence:** Vue component coverage and a real browser response both show the response facts, 7 Evidence rows, and 6 Trace steps.

## Existing Java Build Output Was Locked

- **Symptom:** source-tree Java test compilation could not replace `apps/mall-api/target/classes/application.yml`.
- **Root cause:** an earlier, unrelated local Java service on port 8081 held a file handle in the source-tree `target` directory.
- **Fix:** no process was terminated and no project build output was deleted. Java tests and the 8091 validation API used isolated temporary source copies with `target` excluded.
- **Regression evidence:** 25 Java tests passed, and Flyway V1-V8 applied in an empty H2 test database.

## Initial Terminal API Probe Reported Supported Questions as Unsupported

- **Symptom:** a PowerShell probe with directly embedded Chinese literals returned `UNSUPPORTED_QUESTION` for price and purchasability prompts.
- **Root cause:** the command transport encoded those literals incorrectly before the request reached the local provider. It was not a Java or Python intent-classification defect.
- **Fix:** repeat the same local API matrix with UTF-8-safe request strings. Price, specification, SKU-code, purchasability, and zero-stock questions returned `ANSWERED`; only shipping returned `UNSUPPORTED_QUESTION`.
- **Regression evidence:** the browser Chinese question and the UTF-8-safe local API matrix both returned grounded normal responses.

## Trace Panel Initially Extended Beyond the Primary Screenshot View

- **Symptom:** the six real Trace steps were not comfortably visible at the primary desktop height.
- **Root cause:** right-panel spacing was too loose for the full Evidence and Trace payload.
- **Fix:** tighten only P4C right-panel typography and vertical spacing; no content was removed or hidden.
- **Regression evidence:** the final normal screenshot visibly includes all six steps.

