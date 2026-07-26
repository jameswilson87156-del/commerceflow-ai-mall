# P4 AI Safety Boundary

## Input Rules

- Trim and validate question length from 1 to 500 characters; reject blank or longer input before trace/provider work.
- Treat question text as untrusted data. It cannot override Java facts, system rules, provider mode, or endpoint behavior.
- Plain-text render all questions and answers in Vue; no HTML or script rendering.
- Validate `userId`, `productId`, `skuId`, relation consistency, and `clientRequestId` at Java's public boundary.

## Prompt-injection and Fact Rules

Requests to ignore facts, reveal a system prompt, change stock/price, create an order, expose another user's order, or invent a policy return `UNSUPPORTED_QUESTION` or a fixed safe boundary answer. Python receives only Java's allowlisted `businessFacts` and cannot access the database.

## Output Rules

Java validates Python's schema, trace id, answer-status enum, provider fields, answer length, and response completeness. Java creates the final Evidence list from database facts. Missing/invalid provider JSON never becomes an apparently successful ordinary answer.

## Privacy and Operations

No API key, authorization header, full sensitive prompt, order/address data, raw provider response, or stack trace enters `businessFacts`, browser response, or persistent trace. Provider calls have finite timeout. P4 does not implement Redis rate limiting, queues, agents, tools, database writes, payments, or order lookup.
