# CommerceFlow AI Customer Service Evaluation V1

## Dataset

- File: `services/ai-service/evaluation/customer-service-cases-v1.json`
- Version: `customer-service-synthetic-v1`
- 20 synthetic cases: four each for price, specification, SKU code, stock/purchase, and unsupported questions.
- Business facts use a synthetic product and SKU. No customer, order, or production data is included.
- The expected outcome checks the answer status and a fact-derived answer fragment.

## Local baseline

Command, from `services/ai-service`:

```powershell
py -3.12 .\evaluate_provider.py
```

Observed local Mock baseline: **20/20 scenarios accepted**. This checks the deterministic local intent rules and server-side fact rendering only. It is not an LLM quality result and must not be presented as one.

Machine-readable output is written to the ignored build directory `services/ai-service/target/evaluation/customer-service-eval-v1.json`.

## External model run

The evaluator supports DeepSeek through its OpenAI-compatible Chat Completions API. External calls require both `COMMERCEFLOW_AI_EVAL_ENABLED=true` and the `--allow-external` flag, plus complete `AI_PROVIDER_*` settings. One model request is sent per synthetic case and may incur provider charges. The result records the provider mode and model name without storing the API key.

One real DeepSeek smoke request is recorded in [deepseek-synthetic-smoke-20260905.md](../deepseek-synthetic-smoke-20260905.md). It used the then-current `deepseek-chat` model name and proves a single local request path only. The full 20-case DeepSeek evaluation has not been run, so there is no multi-case model-quality score to report. For a current run, use `https://api.deepseek.com` and `deepseek-flash`.

## Limits

This small, curated synthetic set is useful for a repeatable local comparison. It does not represent a live catalog distribution, customer traffic, production response quality, or a safety guarantee.
