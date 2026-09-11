import asyncio

from app.models.c01_u01 import C01U01Request
from app.services.c01_u01_service import C01U01ClinicalUnderstandingService


BINDING_ID = C01U01ClinicalUnderstandingService.BINDING_ID
CAPABILITY_SET_VERSION = C01U01ClinicalUnderstandingService.CAPABILITY_SET_VERSION
SCOPE_VERSION = C01U01ClinicalUnderstandingService.SCOPE_VERSION
CONTRACT_VERSION = C01U01ClinicalUnderstandingService.CONTRACT_VERSION


def request(text: str, binding_id: str = BINDING_ID) -> C01U01Request:
    return C01U01Request(
        userId="user-1",
        text=text,
        consultationId="consult-test",
        knownSubjectReferenceId=None,
        bindingId=binding_id,
        capabilitySetVersion=CAPABILITY_SET_VERSION,
        scopeVersion=SCOPE_VERSION,
        contractVersion=CONTRACT_VERSION,
    )


def interpret(text: str):
    service = C01U01ClinicalUnderstandingService()
    return asyncio.run(service.interpret(request(text)))


def test_self_clinical_complaint_produces_candidates_not_truth():
    result = interpret("我持续咳嗽三天")
    assert result.businessStatus == "SUCCESS"
    assert result.subjectCandidate.subjectType == "SELF"
    assert result.scopeCandidate.scope == "SYMPTOM"
    assert result.problemCandidate.text == "我持续咳嗽三天"
    assert result.bindingRef.bindingStatus == "ACTIVE"
    assert result.bindingRef.capabilityId == "C01"
    assert not hasattr(result, "suggested_writes")


def test_other_relation_is_candidate_and_does_not_forge_subject_reference_id():
    result = interpret("我妈这两天头晕")
    assert result.subjectCandidate.subjectType == "OTHER"
    assert result.subjectCandidate.relationText in ("我妈", "妈妈", "母亲")
    # Formal subjectReferenceId belongs to U01 identity/business context, not C01 inference.
    assert not hasattr(result.subjectCandidate, "subjectReferenceId")
    assert result.scopeCandidate.scope == "SYMPTOM"


def test_mixed_request_does_not_hide_clinical_discomfort():
    result = interpret("我想制定减肥计划，但最近胸痛")
    assert result.businessStatus == "INSUFFICIENT_INFORMATION"
    assert result.scopeCandidate.scope == "MIXED"
    assert result.scopeCandidate.uncertain is True
    assert result.earlySafetySignalCandidate.detected is True
    assert "胸痛" in result.earlySafetySignalCandidate.clues


def test_outside_v1_intent_is_candidate_not_failure():
    result = interpret("我想制定减肥计划")
    assert result.businessStatus == "SUCCESS"
    assert result.scopeCandidate.scope == "OUTSIDE_V1_INTENT"
    assert result.retryable is False


def test_vague_input_remains_unknown_instead_of_guessing():
    result = interpret("你好")
    assert result.businessStatus == "INSUFFICIENT_INFORMATION"
    assert result.subjectCandidate.subjectType == "UNKNOWN"
    assert result.scopeCandidate.scope == "UNKNOWN"
    assert result.problemCandidate.text is None


def test_binding_mismatch_fails_closed_as_unsupported():
    service = C01U01ClinicalUnderstandingService()
    result = asyncio.run(service.interpret(request("我头晕", binding_id="unapproved-binding")))
    assert result.businessStatus == "UNSUPPORTED"
    assert result.reasonCode == "CAPABILITY_BINDING_MISMATCH"
    assert result.scopeCandidate.scope == "UNKNOWN"
    assert result.retryable is False


def test_empty_input_is_insufficient_information_not_negative_clinical_result():
    result = interpret("   ")
    assert result.businessStatus == "INSUFFICIENT_INFORMATION"
    assert result.reasonCode == "EMPTY_USER_INPUT"
    assert result.scopeCandidate.scope == "UNKNOWN"
    assert result.earlySafetySignalCandidate.detected is False
