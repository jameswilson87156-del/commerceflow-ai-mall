# P4B Security Evidence

## Input and Output

Java rejects non-positive ids, blank questions, questions longer than 500 characters, malformed client request ids, missing Product/SKU values, and mismatched Product/SKU selections. Python has strict Pydantic models with unknown fields forbidden.

The Python tests exercise prompt-injection wording, system-prompt requests, stock/price-change attempts, order-privacy questions, and script-like input. They produce `UNSUPPORTED_QUESTION`; no tool, write, database, or policy answer is attempted.

## Data Boundary

Only selected Product/SKU/Inventory facts, an empty bounded knowledge list, and Java query time cross to Python. Database credentials, SQL, authorization, API keys, system prompts, carts, addresses, and orders are excluded. Python has no database driver/configuration or Java callback.

## Sensitive-data Check

A repository scan of tracked/untracked P4B paths found no real `.env`, token, private-key, database, `node_modules`, or `target` file to commit. `.env.example` contains only local empty provider placeholders. P4B did not call an external provider and did not use a real API key.

## Persistence

Trace stores a category summary instead of the raw question. It stores no provider raw JSON, prompt, authorization header, secret, address, order content, or stack trace. Its only data mutation is the intended `ai_trace` summary row.
