import json

import httpx
import pytest

from app.config import ConfigSource, ProviderConfig, REMOTE_PROVIDER_TIMEOUT_BUDGET, resolve_provider_config
from app.models import CustomerServiceRequest
from app.providers import ProviderFailure, ProviderRouter, _chat_completions_url, _system_prompt


def request(question: str = "What is the SKU code?", **fact_overrides: object) -> CustomerServiceRequest:
    facts: dict[str, object] = {
        "question": question,
        "productId": 101,
        "productCode": "PROD-1001",
        "productName": "Synthetic T-shirt",
        "productStatus": "ON_SALE",
        "productImagePath": "/assets/products/product-tshirt-gray.png",
        "skuId": 10004,
        "skuCode": "T-SHIRT-GRAY-L",
        "color": "Gray",
        "size": "L",
        "skuStatus": "ON_SALE",
        "unitPrice": "129.00",
        "currency": "CNY",
        "availableStock": 27,
        "knowledgeSnippets": [],
        "queriedAt": "2026-07-26T09:00:00Z",
    }
    facts.update(fact_overrides)
    return CustomerServiceRequest.model_validate({
        "traceId": "ai12-trace-001", "clientRequestId": "ai12-test-001", "question": question, "businessFacts": facts,
    })


def real_config() -> ProviderConfig:
    return ProviderConfig(ConfigSource.PROJECT_OVERRIDE, "REAL_OPENAI_COMPATIBLE", "CHAT_COMPLETIONS",
                          "https://stub.invalid/v1", "synthetic-test-model", "test-not-secret", True)


def remote_content(req: CustomerServiceRequest, intent: str = "SKU_CODE", status: str = "ANSWERED", **overrides: object) -> str:
    body: dict[str, object] = {"intent": intent, "answerStatus": status, "claims": req.businessFacts.model_dump(mode="json")}
    body.update(overrides)
    return json.dumps(body)


def completion(content: str, status: int = 200) -> httpx.Response:
    return httpx.Response(status, json={"choices": [{"message": {"content": content}}]})


def shared_real_env() -> dict[str, str]:
    return {
        "PORTFOLIO_AI_PROVIDER": "openai-compatible", "PORTFOLIO_AI_BASE_URL": "https://stub.invalid",
        "PORTFOLIO_AI_MODEL": "shared-model", "PORTFOLIO_AI_API_KEY": "shared-key", "PORTFOLIO_AI_PROTOCOL": "chat-completions",
    }


def test_default_without_environment_is_local_mock() -> None:
    config = resolve_provider_config({})
    assert config.source == ConfigSource.MOCK_DEFAULT
    assert config.mode == "MOCK"


def test_field_level_fallback_keeps_shared_real_configuration_when_project_only_overrides_fallback() -> None:
    env = shared_real_env() | {"COMMERCEFLOW_AI_FALLBACK_ENABLED": "false"}
    config = resolve_provider_config(env)
    assert config.source == ConfigSource.MIXED_LAYERED
    assert config.mode == "REAL_OPENAI_COMPATIBLE"
    assert config.protocol == "CHAT_COMPLETIONS"
    assert config.fallback_enabled is False
    assert config.model == "shared-model"


def test_project_model_and_key_overrides_are_field_level_without_printing_secrets() -> None:
    env = shared_real_env() | {"COMMERCEFLOW_AI_MODEL": "project-model", "COMMERCEFLOW_AI_API_KEY": "project-key"}
    config = resolve_provider_config(env)
    assert config.source == ConfigSource.MIXED_LAYERED
    assert config.model == env["COMMERCEFLOW_AI_MODEL"]
    assert config.api_key == env["COMMERCEFLOW_AI_API_KEY"]
    assert config.base_url == env["PORTFOLIO_AI_BASE_URL"]


def test_empty_project_values_do_not_mask_shared_and_legacy_is_last_layer() -> None:
    env = shared_real_env() | {"COMMERCEFLOW_AI_PROVIDER": " ", "COMMERCEFLOW_AI_MODEL": ""}
    config = resolve_provider_config(env)
    assert config.source == ConfigSource.SHARED_PORTFOLIO
    assert config.mode == "REAL_OPENAI_COMPATIBLE"
    legacy = {
        "AI_PROVIDER_MODE": "openai-compatible", "AI_PROVIDER_BASE_URL": "https://stub.invalid",
        "AI_PROVIDER_MODEL": "legacy-model", "AI_PROVIDER_API_KEY": "legacy-key", "AI_PROVIDER_PROTOCOL": "chat-completions",
    }
    assert resolve_provider_config(legacy).source == ConfigSource.LEGACY_COMPATIBILITY


@pytest.mark.parametrize(("base_url", "expected"), [
    ("https://stub.invalid", "https://stub.invalid/v1/chat/completions"),
    ("https://stub.invalid/v1", "https://stub.invalid/v1/chat/completions"),
    ("https://stub.invalid/chat/completions", "https://stub.invalid/v1/chat/completions"),
    ("https://stub.invalid/v1/chat/completions", "https://stub.invalid/v1/chat/completions"),
    ("http://localhost:8080/v1", "http://localhost:8080/v1/chat/completions"),
    ("http://127.0.0.1:8080", "http://127.0.0.1:8080/v1/chat/completions"),
    ("http://[::1]:8080/chat/completions", "http://[::1]:8080/v1/chat/completions"),
])
def test_chat_completions_url_normalizes_https_and_local_http(base_url: str, expected: str) -> None:
    assert _chat_completions_url(base_url) == expected


@pytest.mark.parametrize("base_url", [
    "http://remote.invalid",
    "http://remote.invalid/v1",
    "https://user:password@stub.invalid",
    "https://stub.invalid/v1?token=redacted",
    "https://stub.invalid/v1#fragment",
])
def test_chat_completions_url_rejects_insecure_or_ambiguous_urls(base_url: str) -> None:
    with pytest.raises(ProviderFailure) as failure:
        _chat_completions_url(base_url)
    assert failure.value.code == "PROVIDER_CONFIGURATION_ERROR"
    assert base_url not in failure.value.message


def test_unknown_provider_and_protocol_fail_closed() -> None:
    assert resolve_provider_config({"PORTFOLIO_AI_PROVIDER": "unknown"}).error_code == "PROVIDER_MODE_UNSUPPORTED"
    assert resolve_provider_config(shared_real_env() | {"COMMERCEFLOW_AI_PROTOCOL": "responses"}).error_code == "PROVIDER_PROTOCOL_UNSUPPORTED"


def test_request_uses_only_model_and_messages_and_prompt_declares_full_schema() -> None:
    seen: list[httpx.Request] = []

    def handler(http_request: httpx.Request) -> httpx.Response:
        seen.append(http_request)
        return completion(remote_content(request()))

    answer = ProviderRouter(config=real_config(), transport=httpx.MockTransport(handler)).answer(request())
    assert answer.answer == "该 SKU 编码为 T-SHIRT-GRAY-L。"
    payload = json.loads(seen[0].content)
    assert set(payload) == {"model", "messages"}
    prompt = payload["messages"][0]["content"]
    for field in request().businessFacts.model_dump().keys():
        assert field in prompt
    for enum_value in ("PRICE", "SPECIFICATION", "SKU_CODE", "STOCK_OR_PURCHASE", "UNSUPPORTED", "ANSWERED", "UNSUPPORTED_QUESTION"):
        assert enum_value in prompt
    assert "Markdown" in prompt
    assert "test-not-secret" not in prompt
    assert "stub.invalid" not in prompt
    assert "COMMERCEFLOW_AI_" not in prompt


def test_remote_free_text_is_not_used_or_allowed_in_structured_response() -> None:
    body = json.loads(remote_content(request()))
    body["answer"] = "incorrect free text"
    router = ProviderRouter(config=real_config(), transport=httpx.MockTransport(lambda _: completion(json.dumps(body))))
    with pytest.raises(ProviderFailure) as failure:
        router.answer(request())
    assert failure.value.code == "PROVIDER_INVALID_SCHEMA"
    assert "incorrect free text" not in failure.value.message


@pytest.mark.parametrize(("question", "intent", "fragment"), [
    ("How much is it?", "PRICE", "¥129.00"),
    ("What color and size?", "SPECIFICATION", "Gray L"),
    ("What is the SKU code?", "SKU_CODE", "T-SHIRT-GRAY-L"),
    ("Can I buy it?", "STOCK_OR_PURCHASE", "27 \u4ef6"),
])
def test_valid_claims_are_rendered_server_side_for_each_supported_intent(question: str, intent: str, fragment: str) -> None:
    req = request(question)
    router = ProviderRouter(config=real_config(), transport=httpx.MockTransport(lambda _: completion(remote_content(req, intent))))
    answer = router.answer(req)
    assert fragment in answer.answer
    assert answer.answerStatus.value == "ANSWERED"


def test_off_sale_zero_stock_and_unsupported_are_rendered_from_facts_only() -> None:
    off_sale = request("Can I buy it?", productStatus="OFF_SALE")
    zero_stock = request("Can I buy it?", availableStock=0)
    unsupported = request("When will shipping happen?")
    for req, intent, status, fragment in (
        (off_sale, "STOCK_OR_PURCHASE", "ANSWERED", "无法购买"),
        (zero_stock, "STOCK_OR_PURCHASE", "ANSWERED", "库存为 0"),
        (unsupported, "UNSUPPORTED", "UNSUPPORTED_QUESTION", "暂不支持"),
    ):
        answer = ProviderRouter(config=real_config(), transport=httpx.MockTransport(
            lambda _, req=req, intent=intent, status=status: completion(remote_content(req, intent, status)))).answer(req)
        assert answer.answerStatus.value == status
        assert fragment in answer.answer


def test_intent_mismatch_is_rejected_before_any_answer_is_returned() -> None:
    req = request("What is the SKU code?")
    router = ProviderRouter(config=real_config(), transport=httpx.MockTransport(lambda _: completion(remote_content(req, "PRICE"))))
    with pytest.raises(ProviderFailure) as failure:
        router.answer(req)
    assert failure.value.code == "PROVIDER_FACT_MISMATCH"


@pytest.mark.parametrize(("status", "expected"), [
    (401, "REMOTE_PROVIDER_AUTH_REJECTED"), (403, "REMOTE_PROVIDER_AUTH_REJECTED"),
    (404, "REMOTE_PROVIDER_NOT_FOUND"), (429, "REMOTE_PROVIDER_RATE_LIMITED"), (500, "REMOTE_PROVIDER_SERVER_ERROR"),
])
def test_http_failures_are_typed_without_exposing_response_body(status: int, expected: str) -> None:
    router = ProviderRouter(config=real_config(), transport=httpx.MockTransport(lambda _: httpx.Response(status, text="upstream-body-must-not-leak")))
    with pytest.raises(ProviderFailure) as failure:
        router.answer(request())
    assert failure.value.code == expected
    assert "upstream-body" not in failure.value.message


def test_connection_timeout_and_timeout_budget_are_bounded() -> None:
    assert REMOTE_PROVIDER_TIMEOUT_BUDGET.read_seconds == 10.0
    assert REMOTE_PROVIDER_TIMEOUT_BUDGET.connect_seconds == 2.0
    for exc, expected in ((httpx.ConnectError("offline"), "REMOTE_PROVIDER_UNAVAILABLE"), (httpx.ReadTimeout("slow"), "REMOTE_PROVIDER_TIMEOUT")):
        router = ProviderRouter(config=real_config(), transport=httpx.MockTransport(lambda _: (_ for _ in ()).throw(exc)))
        with pytest.raises(ProviderFailure) as failure:
            router.answer(request())
        assert failure.value.code == expected


@pytest.mark.parametrize("content", [
    "not-json", "```json\n{}\n```", json.dumps({"intent": "SKU_CODE", "answerStatus": "ANSWERED"}),
    json.dumps({"intent": "SKU_CODE", "answerStatus": "ANSWERED", "claims": {}, "extra": True}),
])
def test_invalid_markdown_missing_and_extra_structured_content_fails(content: str) -> None:
    router = ProviderRouter(config=real_config(), transport=httpx.MockTransport(lambda _: completion(content)))
    with pytest.raises(ProviderFailure) as failure:
        router.answer(request())
    assert failure.value.code == "PROVIDER_INVALID_SCHEMA"


def test_claim_mismatch_fails_validation() -> None:
    body = json.loads(remote_content(request()))
    body["claims"]["availableStock"] = 999
    router = ProviderRouter(config=real_config(), transport=httpx.MockTransport(lambda _: completion(json.dumps(body))))
    with pytest.raises(ProviderFailure) as failure:
        router.answer(request())
    assert failure.value.code == "PROVIDER_FACT_MISMATCH"


def test_mock_never_calls_remote_transport() -> None:
    called = False

    def handler(_: httpx.Request) -> httpx.Response:
        nonlocal called
        called = True
        return completion(remote_content(request()))

    mock = ProviderRouter(config=ProviderConfig(ConfigSource.MOCK_DEFAULT, "MOCK", None, None, None, None, True),
                          transport=httpx.MockTransport(handler))
    assert mock.answer(request()).provider.mode == "MOCK"
    assert not called
