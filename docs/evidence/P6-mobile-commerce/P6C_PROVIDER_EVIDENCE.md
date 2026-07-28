# P6C Provider Evidence

The normal local provider response was `commerceflow-mock / MOCK`. It is a local deterministic FastAPI provider and did not use an external model or API key.

With the local Python service stopped, Java returned `java-fact-fallback / FALLBACK`, `fallbackUsed: true`, and a fact-based answer for the same SKU. The mobile page labels this as a Java fact fallback rather than presenting it as a model answer.

The mobile client never calls Python directly and never sends a full chat history to Java or Python.
