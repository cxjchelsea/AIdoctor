import asyncio
import inspect

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.api.c01_routes import router as c01_router
from app.models.c01_u02 import C01U02Request
from app.services.c01_u02_service import C01U02ClinicalUnderstandingService


class FakeRecognizer:
    def recognize(self, text):
        concepts = []
        if "头晕" in text:
            concepts.append({"standard_term": "头晕", "original_text": "头晕", "concept_type": "symptom", "cui": "C0012833", "confidence": 0.9})
        if "体温" in text:
            concepts.append({"standard_term": "体温", "original_text": "体温", "concept_type": "indicator", "loinc": "8310-5", "confidence": 0.9})
        return concepts


class FakeAmbiguityDetector:
    def detect(self, text, concepts):
        return ["ambiguous"] if "可能" in text else []


def request(text, binding_id=None, source_type="PATIENT_REPORTED"):
    service = C01U02ClinicalUnderstandingService
    return C01U02Request(
        userId="user-1",
        text=text,
        consultationId="consult-1",
        eventId="event-1",
        sourceType=source_type,
        bindingId=binding_id or service.BINDING_ID,
        capabilitySetVersion=service.CAPABILITY_SET_VERSION,
        scopeVersion=service.SCOPE_VERSION,
        contractVersion=service.CONTRACT_VERSION,
    )


def interpret(text, source_type="PATIENT_REPORTED"):
    service = C01U02ClinicalUnderstandingService()
    service._concept_recognizer = FakeRecognizer()
    service._ambiguity_detector = FakeAmbiguityDetector()
    return asyncio.run(service.interpret(request(text, source_type=source_type)))


def test_negation_stays_no_candidate_not_committed_truth():
    result = interpret("我没有头晕")
    observation = result.observationCandidates[0]
    assert result.businessStatus == "SUCCESS"
    assert observation.valueSemantics == "NO"
    assert observation.negation is True
    assert observation.lifecycle == "NORMALIZED"
    assert not hasattr(result, "stateChangeProposal")
    assert not hasattr(result, "suggested_writes")


def test_unknown_is_not_no():
    result = interpret("我不确定有没有头晕")
    observation = result.observationCandidates[0]
    assert observation.valueSemantics == "UNKNOWN"
    assert observation.valueSemantics != "NO"
    assert observation.lifecycle == "UNCERTAIN"
    assert "USER_UNCERTAIN" in observation.ambiguityFlags


def test_unmeasured_is_not_normal_and_keeps_source_type():
    result = interpret("体温还没测", source_type="PATIENT_REPORTED")
    observation = result.observationCandidates[0]
    assert observation.valueSemantics == "UNMEASURED"
    assert observation.valueSemantics != "NORMAL"
    assert observation.sourceType == "PATIENT_REPORTED"


def test_value_unit_temporality_and_severity_are_candidate_metadata():
    result = interpret("今天体温39.2℃，症状严重")
    observation = result.observationCandidates[0]
    assert observation.normalizedValue == "39.2"
    assert observation.unit == "℃"
    assert observation.temporality == "CURRENT"
    assert observation.severityOrDegree is None


def test_mixed_input_does_not_leak_negation_or_measurement_between_concepts():
    result = interpret("没有头晕，今天体温38.5℃")
    observations = {item.conceptDisplay: item for item in result.observationCandidates}

    dizziness = observations["头晕"]
    temperature = observations["体温"]

    assert dizziness.valueSemantics == "NO"
    assert dizziness.negation is True
    assert dizziness.normalizedValue is None
    assert dizziness.unit is None
    assert dizziness.temporality is None

    assert temperature.valueSemantics == "YES"
    assert temperature.negation is False
    assert temperature.normalizedValue == "38.5"
    assert temperature.unit == "℃"
    assert temperature.temporality == "CURRENT"


def test_unresolved_expression_fails_soft_without_guessing_concept():
    result = interpret("有点说不上来的感觉")
    observation = result.observationCandidates[0]
    assert result.businessStatus == "INSUFFICIENT_INFORMATION"
    assert observation.conceptId is None
    assert observation.valueSemantics == "UNKNOWN"
    assert observation.lifecycle == "UNCERTAIN"


def test_binding_mismatch_fails_closed():
    service = C01U02ClinicalUnderstandingService()
    result = asyncio.run(service.interpret(request("我头晕", binding_id="wrong-binding")))
    assert result.businessStatus == "UNSUPPORTED"
    assert result.reasonCode == "CAPABILITY_BINDING_MISMATCH"
    assert result.observationCandidates == []


def test_http_contract_is_candidate_only():
    app = FastAPI()
    app.include_router(c01_router, prefix="/api/v1")
    client = TestClient(app)
    response = client.post("/api/v1/capabilities/c01/u02/interpret", json=request("").model_dump())
    assert response.status_code == 200
    body = response.json()
    assert body["reasonCode"] == "EMPTY_CLINICAL_INPUT"
    assert "stateChangeProposal" not in body
    assert "commit" not in body
    assert "suggested_writes" not in body


def test_service_has_no_clinical_state_write_or_legacy_orchestration_reference():
    source = inspect.getsource(C01U02ClinicalUnderstandingService)
    names = C01U02ClinicalUnderstandingService.interpret.__code__.co_names
    assert "CDPManager" not in source
    assert "DiagnosisWorkflowOrchestrator" not in source
    assert "StateCommitter" not in names
    assert "suggested_writes" not in source
