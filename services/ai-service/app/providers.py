import os
from typing import Protocol

from app.models import AnswerStatus, CustomerServiceRequest, CustomerServiceResponse, Provider


class ProviderFailure(Exception):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.message = message


class CustomerServiceProvider(Protocol):
    def answer(self, request: CustomerServiceRequest) -> CustomerServiceResponse:
        ...


class CommerceFlowMockProvider:
    """Deterministic, local-only provider. It only uses Java-owned business facts."""

    name = "commerceflow-mock"

    def answer(self, request: CustomerServiceRequest) -> CustomerServiceResponse:
        facts = request.businessFacts
        question = request.question.strip().lower()

        if self._is_unsupported(question):
            return self._response(
                request,
                "当前商品客服只支持价格、颜色尺码、库存、是否可购买和 SKU 编码问题；暂不支持该问题。",
                AnswerStatus.UNSUPPORTED_QUESTION,
            )
        if facts.productStatus != "ON_SALE" or facts.skuStatus != "ON_SALE":
            return self._response(request, "该商品当前不是上架销售状态，暂时无法购买。", AnswerStatus.ANSWERED)
        if self._mentions(question, "sku", "编码", "货号"):
            return self._response(request, f"该 SKU 编码为 {facts.skuCode}。", AnswerStatus.ANSWERED)
        if self._mentions(question, "颜色", "尺码", "尺寸", "规格"):
            return self._response(request, f"当前选择的 SKU 为 {facts.color} {facts.size}，SKU 编码为 {facts.skuCode}。", AnswerStatus.ANSWERED)
        if self._mentions(question, "价格", "售价", "多少钱", "价钱"):
            return self._response(request, f"{facts.productName} {facts.color} {facts.size} 的售价为 ¥{facts.unitPrice:.2f}。", AnswerStatus.ANSWERED)
        if self._mentions(question, "库存", "有货", "购买", "能买", "可买", "下单"):
            if facts.availableStock == 0:
                return self._response(request, "该 SKU 当前库存为 0，暂时无法购买。", AnswerStatus.ANSWERED)
            return self._response(
                request,
                f"{facts.color} {facts.size} 码当前库存为 {facts.availableStock} 件，可以购买，售价为 ¥{facts.unitPrice:.2f}。",
                AnswerStatus.ANSWERED,
            )
        return self._response(
            request,
            "当前商品客服只支持价格、颜色尺码、库存、是否可购买和 SKU 编码问题；暂不支持该问题。",
            AnswerStatus.UNSUPPORTED_QUESTION,
        )

    def _response(self, request: CustomerServiceRequest, answer: str, status: AnswerStatus) -> CustomerServiceResponse:
        return CustomerServiceResponse(
            traceId=request.traceId,
            answer=answer,
            answerStatus=status,
            provider=Provider(name=self.name, mode="MOCK", model=None),
            warning=None,
        )

    def _is_unsupported(self, question: str) -> bool:
        return self._mentions(
            question,
            "发货", "物流", "快递", "退款", "退货", "支付", "付款", "优惠", "折扣", "订单",
            "其他用户", "忽略之前", "system prompt", "系统提示", "提示词", "改价", "修改价格", "改库存",
            "修改库存", "<script", "javascript:",
        )

    def _mentions(self, question: str, *terms: str) -> bool:
        return any(term in question for term in terms)


class RealProviderPlaceholder:
    """Reserved for a separately approved, schema-bound remote provider integration."""

    def answer(self, request: CustomerServiceRequest) -> CustomerServiceResponse:
        raise ProviderFailure("PROVIDER_CONFIGURATION_ERROR", "真实 AI Provider 尚未在本地 Showcase 中启用。")


class ProviderRouter:
    def __init__(self, mode: str | None = None) -> None:
        self.mode = (mode or os.getenv("AI_PROVIDER_MODE", "MOCK")).upper()
        self.mock_provider = CommerceFlowMockProvider()
        self.real_provider = RealProviderPlaceholder()

    def answer(self, request: CustomerServiceRequest) -> CustomerServiceResponse:
        if self.mode == "MOCK":
            return self.mock_provider.answer(request)
        if self.mode == "REAL_OPENAI_COMPATIBLE":
            return self.real_provider.answer(request)
        raise ProviderFailure("PROVIDER_MODE_INVALID", "AI_PROVIDER_MODE 必须为 MOCK 或 REAL_OPENAI_COMPATIBLE。")
