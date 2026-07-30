import json
from typing import Protocol
from urllib.parse import urlsplit, urlunsplit

import httpx

from app.config import REMOTE_PROVIDER_TIMEOUT_BUDGET, ProviderConfig, resolve_provider_config
from app.models import AnswerStatus, CustomerServiceRequest, CustomerServiceResponse, Provider, RemoteStructuredAnswer


class ProviderFailure(Exception):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.message = message


class CustomerServiceProvider(Protocol):
    def answer(self, request: CustomerServiceRequest) -> CustomerServiceResponse:
        ...


_INTENTS = ("PRICE", "SPECIFICATION", "SKU_CODE", "STOCK_OR_PURCHASE", "UNSUPPORTED")
_ANSWER_STATUSES = ("ANSWERED", "UNSUPPORTED_QUESTION")
_UNSUPPORTED_TERMS = (
    "shipping", "logistics", "refund", "payment", "coupon", "discount", "other user", "order",
    "system prompt", "ignore previous", "modify price", "modify inventory", "javascript", "<script",
    "发货", "物流", "快递", "退款", "退货", "支付", "付款", "优惠", "折扣", "订单", "其他用户",
    "忽略之前", "忽略规则", "系统prompt", "系统提示", "提示词", "改价", "修改价格", "改库存", "修改库存",
)


def classify_question_intent(question: str) -> str:
    """A deliberately small, deterministic classifier for the declared product-support scope."""
    lower = question.strip().lower()
    if any(term in lower for term in _UNSUPPORTED_TERMS):
        return "UNSUPPORTED"
    if any(term in lower for term in ("sku", "编码", "货号")):
        return "SKU_CODE"
    if any(term in lower for term in ("颜色", "尺码", "尺寸", "规格", "color", "size", "specification")):
        return "SPECIFICATION"
    if any(term in lower for term in ("价格", "售价", "多少钱", "价钱", "price", "cost", "how much")):
        return "PRICE"
    if any(term in lower for term in ("库存", "有货", "购买", "能买", "可买", "下单", "stock", "available", "purchase", "buy")):
        return "STOCK_OR_PURCHASE"
    return "UNSUPPORTED"


def render_grounded_answer(intent: str, request: CustomerServiceRequest) -> tuple[str, AnswerStatus]:
    """Build Chinese user-facing text only from Java-owned facts after validation succeeds."""
    facts = request.businessFacts
    if intent == "UNSUPPORTED":
        return "当前商品客服仅支持价格、颜色尺码、库存、是否可购买和 SKU 编码问题；暂不支持该问题。", AnswerStatus.UNSUPPORTED_QUESTION
    if intent == "PRICE":
        return f"{facts.productName} {facts.color} {facts.size} 的售价为 ¥{facts.unitPrice:.2f}。", AnswerStatus.ANSWERED
    if intent == "SPECIFICATION":
        return f"当前 SKU 为 {facts.color} {facts.size}，SKU 编码为 {facts.skuCode}。", AnswerStatus.ANSWERED
    if intent == "SKU_CODE":
        return f"该 SKU 编码为 {facts.skuCode}。", AnswerStatus.ANSWERED
    if intent == "STOCK_OR_PURCHASE":
        if facts.productStatus != "ON_SALE" or facts.skuStatus != "ON_SALE":
            return "该商品当前不是上架销售状态，暂时无法购买。", AnswerStatus.ANSWERED
        if facts.availableStock == 0:
            return "该 SKU 当前库存为 0，暂时无法购买。", AnswerStatus.ANSWERED
        return f"{facts.color} {facts.size} 码当前库存为 {facts.availableStock} 件，可以购买，售价为 ¥{facts.unitPrice:.2f}。", AnswerStatus.ANSWERED
    raise ProviderFailure("PROVIDER_FACT_MISMATCH", "Remote provider intent is not supported.")


def _system_prompt() -> str:
    claims_fields = (
        "question:string, productId:integer, productCode:string|null, productName:string, "
        "productImagePath:string|null, productStatus:ON_SALE|OFF_SALE, skuId:integer, skuCode:string, "
        "color:string, size:string, skuStatus:ON_SALE|OFF_SALE, unitPrice:decimal-string, currency:string, "
        "availableStock:integer, knowledgeSnippets:string[], queriedAt:ISO-8601 datetime"
    )
    return (
        "Return exactly one JSON object and nothing else. Do not use Markdown, code fences, explanations, or extra fields. "
        f"Allowed intent values: {', '.join(_INTENTS)}. Allowed answerStatus values: {', '.join(_ANSWER_STATUSES)}. "
        "Use UNSUPPORTED only with UNSUPPORTED_QUESTION; all other intents require ANSWERED. "
        f"claims must contain exactly these fields and types: {claims_fields}. "
        "Every claims value must be a direct copy of the supplied businessFacts; do not infer, transform, omit, or add facts. "
        "Example shape only (not business data): "
        '{"intent":"PRICE","answerStatus":"ANSWERED","claims":{"question":"q","productId":1,"productCode":null,'
        '"productName":"n","productImagePath":null,"productStatus":"ON_SALE","skuId":1,"skuCode":"s",'
        '"color":"c","size":"z","skuStatus":"ON_SALE","unitPrice":"0.00","currency":"CNY",'
        '"availableStock":0,"knowledgeSnippets":[],"queriedAt":"2026-01-01T00:00:00Z"}}'
    )


class CommerceFlowMockProvider:
    """Deterministic, local-only provider. It only uses Java-owned business facts."""

    name = "commerceflow-mock"

    def answer(self, request: CustomerServiceRequest) -> CustomerServiceResponse:
        intent = classify_question_intent(request.question)
        answer, status = render_grounded_answer(intent, request)
        return CustomerServiceResponse(
            traceId=request.traceId,
            answer=answer,
            answerStatus=status,
            provider=Provider(name=self.name, mode="MOCK", model=None),
            warning=None,
        )


class OpenAICompatibleProvider:
    """One bounded Chat Completions call; Java-owned facts remain authoritative."""

    name = "openai-compatible"

    def __init__(self, config: ProviderConfig, transport: httpx.BaseTransport | None = None) -> None:
        self.config = config
        self.transport = transport

    def answer(self, request: CustomerServiceRequest) -> CustomerServiceResponse:
        if not self.config.base_url or not self.config.model or not self.config.api_key:
            raise ProviderFailure("PROVIDER_CONFIGURATION_ERROR", "Remote provider configuration is incomplete.")
        url = _chat_completions_url(self.config.base_url)
        payload = {
            "model": self.config.model,
            "messages": [
                {"role": "system", "content": _system_prompt()},
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "traceId": request.traceId,
                            "question": request.question,
                            "businessFacts": request.businessFacts.model_dump(mode="json"),
                        },
                        ensure_ascii=False,
                        separators=(",", ":"),
                    ),
                },
            ],
        }
        budget = REMOTE_PROVIDER_TIMEOUT_BUDGET
        try:
            with httpx.Client(
                timeout=httpx.Timeout(connect=budget.connect_seconds, read=budget.read_seconds,
                                     write=budget.write_seconds, pool=budget.pool_seconds),
                transport=self.transport,
                follow_redirects=False,
            ) as client:
                response = client.post(
                    url,
                    headers={"Authorization": f"Bearer {self.config.api_key}", "Content-Type": "application/json"},
                    json=payload,
                )
        except httpx.TimeoutException as exc:
            raise ProviderFailure("REMOTE_PROVIDER_TIMEOUT", "Remote provider timed out.") from exc
        except httpx.RequestError as exc:
            raise ProviderFailure("REMOTE_PROVIDER_UNAVAILABLE", "Remote provider is unavailable.") from exc

        if response.status_code != 200:
            raise ProviderFailure(_http_failure_code(response.status_code), "Remote provider returned an HTTP error.")
        structured = _structured_answer(response)
        _validate_claims_and_intent(structured, request)
        answer, status = render_grounded_answer(structured.intent, request)
        return CustomerServiceResponse(
            traceId=request.traceId,
            answer=answer,
            answerStatus=status,
            provider=Provider(name=self.name, mode="REAL_OPENAI_COMPATIBLE", model=None),
            warning=None,
        )


def _chat_completions_url(base_url: str) -> str:
    try:
        parsed = urlsplit(base_url.strip())
        hostname = parsed.hostname
    except ValueError as exc:
        raise ProviderFailure("PROVIDER_CONFIGURATION_ERROR", "Remote provider base URL is invalid.") from exc
    if (
        parsed.scheme not in {"http", "https"}
        or not parsed.netloc
        or not hostname
        or parsed.username is not None
        or parsed.password is not None
        or parsed.query
        or parsed.fragment
    ):
        raise ProviderFailure("PROVIDER_CONFIGURATION_ERROR", "Remote provider base URL is invalid.")
    normalized_hostname = hostname.lower().rstrip(".")
    if parsed.scheme == "http" and normalized_hostname not in {"localhost", "127.0.0.1", "::1"}:
        raise ProviderFailure("PROVIDER_CONFIGURATION_ERROR", "Remote provider base URL is invalid.")
    parts = [part for part in parsed.path.split("/") if part]
    if len(parts) >= 2 and parts[-2:] == ["chat", "completions"]:
        parts = parts[:-2]
    if parts and parts[-1] == "v1":
        parts = parts[:-1]
    path = "/" + "/".join([*parts, "v1", "chat", "completions"])
    return urlunsplit((parsed.scheme, parsed.netloc, path, "", ""))


def _structured_answer(response: httpx.Response) -> RemoteStructuredAnswer:
    try:
        payload = response.json()
    except ValueError as exc:
        raise ProviderFailure("PROVIDER_INVALID_JSON", "Remote provider response is not JSON.") from exc
    try:
        content = payload["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as exc:
        raise ProviderFailure("PROVIDER_INVALID_SCHEMA", "Remote provider response does not match Chat Completions.") from exc
    if not isinstance(content, str) or content.strip().startswith("```"):
        raise ProviderFailure("PROVIDER_INVALID_SCHEMA", "Remote provider response is not strict JSON.")
    try:
        return RemoteStructuredAnswer.model_validate_json(content)
    except ValueError as exc:
        raise ProviderFailure("PROVIDER_INVALID_SCHEMA", "Remote provider response is not valid structured JSON.") from exc


def _http_failure_code(status_code: int) -> str:
    if status_code in {401, 403}:
        return "REMOTE_PROVIDER_AUTH_REJECTED"
    if status_code == 404:
        return "REMOTE_PROVIDER_NOT_FOUND"
    if status_code == 429:
        return "REMOTE_PROVIDER_RATE_LIMITED"
    if 500 <= status_code <= 599:
        return "REMOTE_PROVIDER_SERVER_ERROR"
    return "REMOTE_PROVIDER_HTTP_ERROR"


def _validate_claims_and_intent(answer: RemoteStructuredAnswer, request: CustomerServiceRequest) -> None:
    expected_intent = classify_question_intent(request.question)
    expected_status = "UNSUPPORTED_QUESTION" if expected_intent == "UNSUPPORTED" else "ANSWERED"
    claims_match = answer.claims.model_dump(mode="json") == request.businessFacts.model_dump(mode="json")
    if not claims_match or answer.intent != expected_intent or answer.answerStatus != expected_status:
        raise ProviderFailure("PROVIDER_FACT_MISMATCH", "Remote provider facts or intent do not match Java-owned facts.")


class ProviderRouter:
    def __init__(self, mode: str | None = None, config: ProviderConfig | None = None, transport: httpx.BaseTransport | None = None) -> None:
        self.config = config or resolve_provider_config()
        if mode is not None:
            self.config = ProviderConfig(self.config.source, _compatibility_mode(mode), self.config.protocol, self.config.base_url,
                                         self.config.model, self.config.api_key, self.config.fallback_enabled,
                                         self.config.error_code)
        self.mode = self.config.mode
        self.mock_provider = CommerceFlowMockProvider()
        self.real_provider = OpenAICompatibleProvider(self.config, transport) if self.mode == "REAL_OPENAI_COMPATIBLE" else None

    def answer(self, request: CustomerServiceRequest) -> CustomerServiceResponse:
        if self.config.error_code:
            raise ProviderFailure(self.config.error_code, "Remote provider configuration is not supported.")
        if self.mode == "MOCK":
            return self.mock_provider.answer(request)
        if self.mode == "REAL_OPENAI_COMPATIBLE" and self.real_provider is not None:
            return self.real_provider.answer(request)
        raise ProviderFailure("PROVIDER_CONFIGURATION_ERROR", "Remote provider configuration is invalid.")


def _compatibility_mode(mode: str) -> str:
    value = mode.strip().lower()
    if value == "mock":
        return "MOCK"
    if value in {"real_openai_compatible", "openai-compatible"}:
        return "REAL_OPENAI_COMPATIBLE"
    return "INVALID"
