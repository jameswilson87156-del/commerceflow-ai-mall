from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class AnswerStatus(str, Enum):
    ANSWERED = "ANSWERED"
    UNSUPPORTED_QUESTION = "UNSUPPORTED_QUESTION"
    INSUFFICIENT_CONTEXT = "INSUFFICIENT_CONTEXT"
    PROVIDER_ERROR = "PROVIDER_ERROR"
    FALLBACK_ANSWER = "FALLBACK_ANSWER"


class BusinessFacts(StrictModel):
    question: str = Field(min_length=1, max_length=500)
    productId: int = Field(gt=0)
    productCode: str | None = Field(default=None, max_length=80)
    productName: str = Field(min_length=1, max_length=180)
    productStatus: Literal["ON_SALE", "OFF_SALE"]
    productImagePath: str | None = Field(default=None, max_length=255)
    skuId: int = Field(gt=0)
    skuCode: str = Field(min_length=1, max_length=80)
    color: str = Field(min_length=1, max_length=60)
    size: str = Field(min_length=1, max_length=40)
    skuStatus: Literal["ON_SALE", "OFF_SALE"]
    unitPrice: Decimal = Field(ge=0, max_digits=19, decimal_places=2)
    currency: str = Field(min_length=1, max_length=10)
    availableStock: int = Field(ge=0)
    knowledgeSnippets: list[str] = Field(default_factory=list)
    queriedAt: datetime


class Provider(StrictModel):
    name: str = Field(min_length=1, max_length=80)
    mode: Literal["MOCK", "REAL_OPENAI_COMPATIBLE", "FALLBACK"]
    model: str | None = Field(default=None, max_length=120)


class CustomerServiceRequest(StrictModel):
    traceId: str = Field(min_length=1, max_length=60)
    clientRequestId: str = Field(pattern=r"^[A-Za-z0-9._-]{1,80}$")
    question: str = Field(min_length=1, max_length=500)
    businessFacts: BusinessFacts


class CustomerServiceResponse(StrictModel):
    traceId: str = Field(min_length=1, max_length=60)
    answer: str = Field(min_length=1, max_length=600)
    answerStatus: AnswerStatus
    provider: Provider
    warning: str | None = Field(default=None, max_length=300)

class RemoteClaims(StrictModel):
    question: str = Field(min_length=1, max_length=500)
    productId: int = Field(gt=0)
    productCode: str | None = Field(default=None, max_length=80)
    productName: str = Field(min_length=1, max_length=180)
    productImagePath: str | None = Field(default=None, max_length=255)
    productStatus: Literal["ON_SALE", "OFF_SALE"]
    skuId: int = Field(gt=0)
    skuCode: str = Field(min_length=1, max_length=80)
    color: str = Field(min_length=1, max_length=60)
    size: str = Field(min_length=1, max_length=40)
    skuStatus: Literal["ON_SALE", "OFF_SALE"]
    unitPrice: Decimal = Field(ge=0, max_digits=19, decimal_places=2)
    currency: str = Field(min_length=1, max_length=10)
    availableStock: int = Field(ge=0)
    knowledgeSnippets: list[str] = Field(default_factory=list)
    queriedAt: datetime


class RemoteStructuredAnswer(StrictModel):
    intent: Literal["PRICE", "SPECIFICATION", "SKU_CODE", "STOCK_OR_PURCHASE", "UNSUPPORTED"]
    answerStatus: Literal["ANSWERED", "UNSUPPORTED_QUESTION"]
    claims: RemoteClaims
