import json
import os
from typing import Any
import httpx
from fastapi import FastAPI
from pydantic import BaseModel, Field

app = FastAPI(title="CommerceFlow AI Service", version="0.1.0")

class BusinessFacts(BaseModel):
    question: str
    productId: int | None = None
    productName: str | None = None
    productStatus: str | None = None
    skuId: int | None = None
    skuCode: str | None = None
    color: str | None = None
    size: str | None = None
    availableStock: int | None = None
    salePrice: str | None = None
    currency: str | None = None
    knowledgeSnippets: list[dict[str, Any]] = Field(default_factory=list)
    queriedAt: str | None = None

class ProductAnswerRequest(BaseModel):
    question: str
    traceId: str
    businessFacts: BusinessFacts

class ProductAnswer(BaseModel):
    traceId: str
    answer: str
    providerMode: str
    structured: dict[str, Any]
    evidence: list[str]
    risk: dict[str, Any]

def mock_answer(request: ProductAnswerRequest) -> ProductAnswer:
    facts = request.businessFacts
    stock = "unknown" if facts.availableStock is None else str(facts.availableStock)
    answer = f"{facts.productName or 'This product'} {facts.color or ''} {facts.size or ''} currently has {stock} units available."
    return ProductAnswer(
        traceId=request.traceId,
        answer=" ".join(answer.split()),
        providerMode="mock",
        structured={"skuCode": facts.skuCode, "availableStock": facts.availableStock, "salePrice": facts.salePrice, "currency": facts.currency},
        evidence=["java.businessFacts"],
        risk={"level": "LOW", "flags": []},
    )

def real_answer(request: ProductAnswerRequest) -> ProductAnswer:
    base_url = os.getenv("AI_PROVIDER_BASE_URL", "").rstrip("/")
    api_key = os.getenv("AI_PROVIDER_API_KEY", "")
    model = os.getenv("AI_PROVIDER_MODEL", "gpt-4o-mini")
    if not base_url or not api_key:
        return mock_answer(request)
    facts = request.businessFacts.model_dump(exclude_none=True)
    prompt = (
        "Answer the customer using only these Java-owned business facts. "
        "Do not invent stock or product data. Return JSON with answer, structured, evidence, risk.\n"
        + json.dumps(facts, ensure_ascii=False)
    )
    try:
        response = httpx.post(
            f"{base_url}/chat/completions",
            headers={"Authorization": f"Bearer {api_key}"},
            json={"model": model, "temperature": 0, "messages": [{"role": "user", "content": prompt}]},
            timeout=4.0,
        )
        response.raise_for_status()
        content = response.json()["choices"][0]["message"]["content"]
        data = json.loads(content)
        return ProductAnswer(
            traceId=request.traceId,
            answer=str(data.get("answer", "The provider returned no answer.")),
            providerMode="openai-compatible",
            structured=data.get("structured", {}),
            evidence=["java.businessFacts", "provider.response"],
            risk=data.get("risk", {"level": "MEDIUM", "flags": ["provider-output"]}),
        )
    except (httpx.HTTPError, KeyError, IndexError, TypeError, json.JSONDecodeError, ValueError):
        return ProductAnswer(
            traceId=request.traceId,
            answer="The configured AI provider returned an invalid or unavailable response; Java business facts remain authoritative.",
            providerMode="openai-compatible",
            structured={"availableStock": request.businessFacts.availableStock, "salePrice": request.businessFacts.salePrice},
            evidence=["java.businessFacts", "provider.failure"],
            risk={"level": "HIGH", "flags": ["provider-failure", "human-review"]},
        )

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "providerMode": os.getenv("AI_PROVIDER_MODE", "mock")}

@app.post("/v1/product-answer", response_model=ProductAnswer)
def product_answer(request: ProductAnswerRequest) -> ProductAnswer:
    # Mock mode is the default. A real provider is opt-in and never becomes the source of business facts.
    if os.getenv("AI_PROVIDER_MODE", "mock").lower() == "real":
        return real_answer(request)
    return mock_answer(request)
