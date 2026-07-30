from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.models import CustomerServiceRequest, CustomerServiceResponse
from app.providers import ProviderFailure, ProviderRouter

app = FastAPI(title="CommerceFlow AI Service", version="0.2.0")
provider_router = ProviderRouter()

_SAFE_FAILURE_MESSAGES = {
    "PROVIDER_CONFIGURATION_ERROR": "Provider configuration is not supported.",
    "PROVIDER_PROTOCOL_UNSUPPORTED": "Provider protocol is not supported.",
    "PROVIDER_MODE_UNSUPPORTED": "Provider mode is not supported.",
    "REMOTE_PROVIDER_TIMEOUT": "Remote provider timed out.",
    "REMOTE_PROVIDER_UNAVAILABLE": "Remote provider is unavailable.",
    "REMOTE_PROVIDER_AUTH_REJECTED": "Remote provider rejected authentication.",
    "REMOTE_PROVIDER_NOT_FOUND": "Remote provider endpoint was not found.",
    "REMOTE_PROVIDER_RATE_LIMITED": "Remote provider rate limit was reached.",
    "REMOTE_PROVIDER_SERVER_ERROR": "Remote provider returned a server error.",
    "REMOTE_PROVIDER_HTTP_ERROR": "Remote provider returned an HTTP error.",
    "PROVIDER_INVALID_JSON": "Remote provider response is not JSON.",
    "PROVIDER_INVALID_SCHEMA": "Remote provider response schema is invalid.",
    "PROVIDER_FACT_MISMATCH": "Remote provider facts did not match.",
    "PROVIDER_INVALID_RESPONSE": "Remote provider response is invalid.",
}


@app.exception_handler(ProviderFailure)
async def provider_failure(_: Request, exc: ProviderFailure) -> JSONResponse:
    message = _SAFE_FAILURE_MESSAGES.get(exc.code, _SAFE_FAILURE_MESSAGES["PROVIDER_INVALID_RESPONSE"])
    return JSONResponse(status_code=503, content={"code": exc.code, "message": message})


@app.get("/health")
def health() -> dict[str, str | bool]:
    config = provider_router.config
    return {
        "status": "UP",
        "provider": "commerceflow-mock" if config.mode == "MOCK" else "openai-compatible",
        "providerMode": config.mode,
        "configSource": config.source.value,
        "adapter": "CHAT_COMPLETIONS" if config.protocol == "CHAT_COMPLETIONS" else "NONE",
        "protocolCompatible": config.error_code is None,
    }


@app.post("/internal/ai/customer-service/answer", response_model=CustomerServiceResponse)
def customer_service_answer(request: CustomerServiceRequest) -> CustomerServiceResponse:
    return provider_router.answer(request)

# Keep the status endpoint free of endpoint, model, and credential values.
# It exposes only the allowed source, adapter, and protocol compatibility metadata.
