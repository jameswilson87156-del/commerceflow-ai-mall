import os
import sys
from pathlib import Path

import pytest

SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

AI_ENVIRONMENT_VARIABLES = (
    "COMMERCEFLOW_AI_PROVIDER", "COMMERCEFLOW_AI_BASE_URL", "COMMERCEFLOW_AI_MODEL", "COMMERCEFLOW_AI_API_KEY",
    "COMMERCEFLOW_AI_PROTOCOL", "COMMERCEFLOW_AI_FALLBACK_ENABLED", "PORTFOLIO_AI_PROVIDER", "PORTFOLIO_AI_BASE_URL",
    "PORTFOLIO_AI_MODEL", "PORTFOLIO_AI_API_KEY", "PORTFOLIO_AI_PROTOCOL", "PORTFOLIO_AI_FALLBACK_ENABLED",
    "AI_PROVIDER_MODE", "AI_PROVIDER_BASE_URL", "AI_PROVIDER_MODEL", "AI_PROVIDER_API_KEY", "AI_PROVIDER_PROTOCOL",
    "AI_PROVIDER_FALLBACK_ENABLED",
)

for _name in AI_ENVIRONMENT_VARIABLES:
    os.environ.pop(_name, None)


@pytest.fixture(autouse=True)
def isolate_provider_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in AI_ENVIRONMENT_VARIABLES:
        monkeypatch.delenv(name, raising=False)
