# Three-Minute Introduction

CommerceFlow AI Mall is an e-commerce and AI customer-service collaboration platform. I chose an ordinary mall business path first because Product, SKU, Inventory, Cart, and Order make correctness visible. The Java modular monolith owns MySQL data and transactions. The first AI flow has Java query current business facts, package them as `businessFacts`, call a Python FastAPI service, and save Evidence and Trace with the answer.

The demo starts with a real local login and catalog, adds a SKU to the cart, submits an order with `Idempotency-Key`, and shows guarded stock deduction and order snapshots. Then I ask the AI support screen about a product. Mock Provider is the default, so the demo runs without a key. The important boundary is that Python cannot write the mall database or invent inventory.

I describe this as a showcase implementation, not as a claim that I hand-wrote every module independently. The follow-up learning rebuild will verify ownership by rewriting the core modules.
