from __future__ import annotations

import argparse
import json
import os
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from app.config import ProviderConfig, resolve_provider_config
from app.models import CustomerServiceRequest
from app.providers import ProviderFailure, ProviderRouter


SERVICE_ROOT = Path(__file__).resolve().parent
DEFAULT_DATASET = SERVICE_ROOT / "evaluation" / "customer-service-cases-v1.json"
DEFAULT_OUTPUT = SERVICE_ROOT / "target" / "evaluation" / "customer-service-eval-v1.json"
PROVIDER_ENVIRONMENT = (
    "AI_PROVIDER_MODE",
    "AI_PROVIDER_BASE_URL",
    "AI_PROVIDER_MODEL",
    "AI_PROVIDER_API_KEY",
    "AI_PROVIDER_PROTOCOL",
    "AI_PROVIDER_FALLBACK_ENABLED",
)


def _provider_config() -> ProviderConfig:
    # Read only the Python AI service provider variables. The Java app's
    # COMMERCEFLOW_AI_PROVIDER=FASTAPI setting is a separate routing switch.
    environment = {name: os.environ[name] for name in PROVIDER_ENVIRONMENT if name in os.environ}
    return resolve_provider_config(environment)


def _request(dataset: dict[str, Any], case: dict[str, Any]) -> CustomerServiceRequest:
    question = case["question"]
    facts = dataset["businessFacts"] | case.get("businessFacts", {}) | {"question": question}
    return CustomerServiceRequest.model_validate({
        "traceId": f"commerceflow-eval-{case['id']}",
        "clientRequestId": f"eval-{case['id']}",
        "question": question,
        "businessFacts": facts,
    })


def _ratio(numerator: int, denominator: int) -> float:
    return round(numerator / denominator, 4) if denominator else 0.0


def evaluate(dataset_path: Path, output_path: Path, allow_external: bool) -> dict[str, Any]:
    dataset = json.loads(dataset_path.read_text(encoding="utf-8"))
    config = _provider_config()
    if config.error_code:
        raise SystemExit(f"Provider configuration is invalid: {config.error_code}")

    external = config.mode == "REAL_OPENAI_COMPATIBLE"
    explicitly_enabled = os.getenv("COMMERCEFLOW_AI_EVAL_ENABLED", "").strip().lower() == "true"
    if external and not (allow_external and explicitly_enabled):
        raise SystemExit(
            "External evaluation is disabled. Set COMMERCEFLOW_AI_EVAL_ENABLED=true "
            "and pass --allow-external to send synthetic cases to the configured model."
        )
    if external:
        print(f"External evaluation enabled: {len(dataset['cases'])} synthetic requests will be sent; provider calls may incur charges.")

    router = ProviderRouter(config=config)
    case_results: list[dict[str, Any]] = []
    per_intent: dict[str, list[bool]] = defaultdict(list)
    failures: Counter[str] = Counter()
    successful_responses = 0

    for case in dataset["cases"]:
        result: dict[str, Any] = {
            "id": case["id"],
            "expectedIntent": case["expectedIntent"],
            "expectedStatus": case["expectedStatus"],
        }
        try:
            response = router.answer(_request(dataset, case))
            successful_responses += 1
            status_match = response.answerStatus.value == case["expectedStatus"]
            answer_match = case["answerContains"] in response.answer
            result.update({
                "actualStatus": response.answerStatus.value,
                "providerMode": response.provider.mode,
                "answerContainsExpectedFact": answer_match,
                "correct": status_match and answer_match,
            })
        except ProviderFailure as exc:
            failures[exc.code] += 1
            result.update({"failureCode": exc.code, "correct": False})

        correct = bool(result["correct"])
        per_intent[case["expectedIntent"]].append(correct)
        case_results.append(result)

    total = len(case_results)
    correct_total = sum(result["correct"] for result in case_results)
    intent_metrics = {
        intent: {
            "total": len(results),
            "correct": sum(results),
            "scenarioAccuracy": _ratio(sum(results), len(results)),
        }
        for intent, results in sorted(per_intent.items())
    }
    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "datasetVersion": dataset["datasetVersion"],
        "dataSource": dataset["dataSource"],
        "provider": {
            "name": "openai-compatible" if external else "commerceflow-mock",
            "mode": config.mode,
            "model": config.model if external else None,
        },
        "datasetSize": total,
        "metrics": {
            "scenarioAccuracy": _ratio(correct_total, total),
            "responseSuccessRate": _ratio(successful_responses, total),
            "perIntent": intent_metrics,
            "providerFailureCodes": dict(sorted(failures.items())),
        },
        "caseResults": case_results,
        "limitations": [
            "Synthetic showcase cases only; results do not represent customer or production traffic.",
            "A Mock run checks local intent and fact-rendering behavior, not language-model quality.",
            "External model runs validate these exact cases only and are not a production quality or safety guarantee.",
        ],
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(
        f"provider={report['provider']['name']} mode={config.mode} "
        f"scenario_accuracy={correct_total}/{total} "
        f"response_success={successful_responses}/{total} output={output_path}"
    )
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate CommerceFlow customer-service provider on synthetic cases.")
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--allow-external", action="store_true", help="Allow external calls when COMMERCEFLOW_AI_EVAL_ENABLED=true.")
    args = parser.parse_args()
    evaluate(args.dataset, args.output, args.allow_external)


if __name__ == "__main__":
    main()
