# Structured fact validation

The adapter accepts only a strict JSON object with a supported intent, answer status, and a complete claim set. The remote object has no free-text `answer` field. Pydantic rejects extra or missing fields, malformed JSON, and Markdown-wrapped JSON.

Claims are compared against the complete Java-provided business facts before a provider answer is returned. Product, SKU, price, stock, status, image-path metadata, knowledge snippets, question, and query time must match exactly. A controlled local classifier also verifies that the remote intent matches the supported question category. A mismatch is typed as `PROVIDER_FACT_MISMATCH` and is not accepted as an answer.

After validation, the service renders the Chinese answer from Java-owned facts for price, specification, SKU code, stock/purchase, or unsupported scope. This closes the possibility of validating claims while displaying unrelated provider free text.

The provider cannot write the business database, alter stock or price, call Java, or become a business-fact source. Java repeats response boundary validation, supplies Evidence from Product/SKU/Inventory, and owns Trace persistence.
