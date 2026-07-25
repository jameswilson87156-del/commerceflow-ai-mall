from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def facts():
    return {
        "question": "Is black M available?", "productId": 101, "productName": "Essential Cotton Shirt",
        "productStatus": "ON_SALE", "skuId": 10001, "skuCode": "SHIRT-BLK-M", "color": "Black",
        "size": "M", "availableStock": 12, "salePrice": "129.00", "currency": "CNY",
        "knowledgeSnippets": [], "queriedAt": "2026-07-25T10:00:00+08:00"
    }

def test_mock_answer_has_evidence_and_structured_data():
    response = client.post('/v1/product-answer', json={"question": "Is black M available?", "traceId": "trace-1", "businessFacts": facts()})
    assert response.status_code == 200
    body = response.json()
    assert body["providerMode"] == "mock"
    assert body["structured"]["availableStock"] == 12
    assert "java.businessFacts" in body["evidence"]

def test_health_runs_without_api_key():
    assert client.get('/health').json()["status"] == "UP"
