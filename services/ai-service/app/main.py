from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.models import CustomerServiceRequest, CustomerServiceResponse
from app.providers import ProviderFailure, ProviderRouter

app = FastAPI(title="CommerceFlow AI Service", version="0.2.0")
provider_router = ProviderRouter()


@app.exception_handler(ProviderFailure)
async def provider_failure(_: Request, exc: ProviderFailure) -> JSONResponse:
    return JSONResponse(status_code=503, content={"code": exc.code, "message": exc.message})


@app.get("/health")
def health() -> dict[str, str]:
    mode = provider_router.mode
    return {
        "status": "UP",
        "provider": "commerceflow-mock" if mode == "MOCK" else "real-provider-placeholder",
        "providerMode": mode,
    }


@app.post("/internal/ai/customer-service/answer", response_model=CustomerServiceResponse)
def customer_service_answer(request: CustomerServiceRequest) -> CustomerServiceResponse:
    return provider_router.answer(request)
