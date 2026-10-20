from __future__ import annotations

import json
from collections import Counter

import pytest

from evaluate_provider import DEFAULT_DATASET, evaluate


def test_dataset_is_balanced_and_uses_only_synthetic_facts() -> None:
    dataset = json.loads(DEFAULT_DATASET.read_text(encoding="utf-8"))
    intents = Counter(case["expectedIntent"] for case in dataset["cases"])

    assert len(dataset["cases"]) == 20
    assert intents == {
        "PRICE": 4,
        "SPECIFICATION": 4,
        "SKU_CODE": 4,
        "STOCK_OR_PURCHASE": 4,
        "UNSUPPORTED": 4,
    }
    assert "synthetic" in dataset["dataSource"]


def test_offline_mock_evaluation_writes_reproducible_report(tmp_path) -> None:
    output = tmp_path / "mock-evaluation.json"

    report = evaluate(DEFAULT_DATASET, output, allow_external=False)

    assert report["provider"] == {"name": "commerceflow-mock", "mode": "MOCK", "model": None}
    assert report["datasetSize"] == 20
    assert report["metrics"]["scenarioAccuracy"] == 1.0
    assert report["metrics"]["responseSuccessRate"] == 1.0
    assert json.loads(output.read_text(encoding="utf-8")) == report


@pytest.mark.parametrize("allow_external", [False, True])
def test_external_evaluation_requires_both_opt_in_controls(monkeypatch, tmp_path, allow_external: bool) -> None:
    monkeypatch.setenv("AI_PROVIDER_MODE", "openai-compatible")
    monkeypatch.setenv("AI_PROVIDER_PROTOCOL", "chat-completions")
    monkeypatch.setenv("AI_PROVIDER_BASE_URL", "https://example.invalid/v1")
    monkeypatch.setenv("AI_PROVIDER_MODEL", "synthetic-test-model")
    monkeypatch.setenv("AI_PROVIDER_API_KEY", "test-key-that-is-never-sent")
    monkeypatch.setenv("COMMERCEFLOW_AI_EVAL_ENABLED", "false")

    with pytest.raises(SystemExit, match="External evaluation is disabled"):
        evaluate(DEFAULT_DATASET, tmp_path / "must-not-run.json", allow_external=allow_external)

    assert not (tmp_path / "must-not-run.json").exists()
