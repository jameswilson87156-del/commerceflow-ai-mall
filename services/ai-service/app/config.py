from __future__ import annotations

import os
from dataclasses import dataclass
from enum import Enum
from typing import Mapping


class ConfigSource(str, Enum):
    PROJECT_OVERRIDE = "PROJECT_OVERRIDE"
    SHARED_PORTFOLIO = "SHARED_PORTFOLIO"
    LEGACY_COMPATIBILITY = "LEGACY_COMPATIBILITY"
    MIXED_LAYERED = "MIXED_LAYERED"
    MOCK_DEFAULT = "MOCK_DEFAULT"


@dataclass(frozen=True)
class ProviderTimeoutBudget:
    """Bounded remote-call budget. Values intentionally contain no endpoint or credential data."""

    connect_seconds: float = 2.0
    read_seconds: float = 10.0
    write_seconds: float = 10.0
    pool_seconds: float = 2.0


REMOTE_PROVIDER_TIMEOUT_BUDGET = ProviderTimeoutBudget()


@dataclass(frozen=True)
class ProviderConfig:
    source: ConfigSource
    mode: str
    protocol: str | None
    base_url: str | None
    model: str | None
    api_key: str | None
    fallback_enabled: bool
    error_code: str | None = None


_PROJECT = {
    "provider": "COMMERCEFLOW_AI_PROVIDER",
    "base_url": "COMMERCEFLOW_AI_BASE_URL",
    "model": "COMMERCEFLOW_AI_MODEL",
    "api_key": "COMMERCEFLOW_AI_API_KEY",
    "protocol": "COMMERCEFLOW_AI_PROTOCOL",
    "fallback_enabled": "COMMERCEFLOW_AI_FALLBACK_ENABLED",
}
_SHARED = {
    "provider": "PORTFOLIO_AI_PROVIDER",
    "base_url": "PORTFOLIO_AI_BASE_URL",
    "model": "PORTFOLIO_AI_MODEL",
    "api_key": "PORTFOLIO_AI_API_KEY",
    "protocol": "PORTFOLIO_AI_PROTOCOL",
    "fallback_enabled": "PORTFOLIO_AI_FALLBACK_ENABLED",
}
_LEGACY = {
    "provider": "AI_PROVIDER_MODE",
    "base_url": "AI_PROVIDER_BASE_URL",
    "model": "AI_PROVIDER_MODEL",
    "api_key": "AI_PROVIDER_API_KEY",
    "protocol": "AI_PROVIDER_PROTOCOL",
    "fallback_enabled": "AI_PROVIDER_FALLBACK_ENABLED",
}
_LAYERS = (
    (ConfigSource.PROJECT_OVERRIDE, _PROJECT),
    (ConfigSource.SHARED_PORTFOLIO, _SHARED),
    (ConfigSource.LEGACY_COMPATIBILITY, _LEGACY),
)


def _value(env: Mapping[str, str], key: str) -> str | None:
    value = env.get(key)
    return value.strip() if value and value.strip() else None


def _resolve_field(env: Mapping[str, str], field: str) -> tuple[str | None, ConfigSource | None]:
    """Resolve one field, so a partial project override never masks shared settings."""
    for source, names in _LAYERS:
        value = _value(env, names[field])
        if value is not None:
            return value, source
    return None, None


def _effective_source(sources: set[ConfigSource]) -> ConfigSource:
    if not sources:
        return ConfigSource.MOCK_DEFAULT
    if len(sources) == 1:
        return next(iter(sources))
    return ConfigSource.MIXED_LAYERED


def _fallback_enabled(raw: str | None) -> bool:
    if raw is None:
        return True
    return raw.lower() not in {"false", "0", "no", "off"}


def _normal_mode(raw: str | None) -> str:
    value = (raw or "").strip().lower()
    if value in {"mock", "local-rule", "local_rule"}:
        return "MOCK"
    if value in {"openai-compatible", "openai_compatible", "real_openai_compatible"}:
        return "REAL_OPENAI_COMPATIBLE"
    return "INVALID"


def _normal_protocol(raw: str | None) -> str | None:
    value = (raw or "").strip().lower()
    if value in {"both", "chat-completions"}:
        return "CHAT_COMPLETIONS"
    if value == "responses":
        return "RESPONSES"
    return None


def resolve_provider_config(env: Mapping[str, str] | None = None) -> ProviderConfig:
    effective = os.environ if env is None else env
    resolved = {field: _resolve_field(effective, field) for field in _PROJECT}
    values = {field: result[0] for field, result in resolved.items()}
    sources = {source for _, source in resolved.values() if source is not None}
    source = _effective_source(sources)
    mode = _normal_mode(values["provider"])
    fallback_enabled = _fallback_enabled(values["fallback_enabled"])

    if not sources:
        return ProviderConfig(ConfigSource.MOCK_DEFAULT, "MOCK", None, None, None, None, True)
    if mode == "MOCK":
        return ProviderConfig(source, mode, None, None, None, None, fallback_enabled)
    if mode != "REAL_OPENAI_COMPATIBLE":
        return ProviderConfig(source, "INVALID", None, None, None, None, fallback_enabled, "PROVIDER_MODE_UNSUPPORTED")

    protocol = _normal_protocol(values["protocol"])
    if protocol != "CHAT_COMPLETIONS":
        return ProviderConfig(source, mode, protocol, None, None, None, fallback_enabled, "PROVIDER_PROTOCOL_UNSUPPORTED")
    if not all(values[name] for name in ("base_url", "model", "api_key")):
        return ProviderConfig(source, mode, protocol, None, None, None, fallback_enabled, "PROVIDER_CONFIGURATION_ERROR")
    return ProviderConfig(source, mode, protocol, values["base_url"], values["model"], values["api_key"], fallback_enabled)
