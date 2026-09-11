"""C01 minimal clinical-understanding slice for U01.

The service produces candidates only. Final Subject/Problem/Scope decisions remain
owned by U01/D10. This adapter deliberately does not produce CDP writes.
"""
from typing import List, Tuple

from app.models.c01_u01 import (
    C01BindingRef,
    C01U01Request,
    C01U01Result,
    EarlySafetySignalCandidate,
    EvidenceSpan,
    ProblemCandidate,
    ScopeCandidate,
    SubjectCandidate,
)
from app.models.request import ClinicalParsingRequest
from app.services.parsing_service import ClinicalParsingService


class C01U01ClinicalUnderstandingService:
    BINDING_ID = "c01-u01-v1-active"
    BINDING_STATUS = "ACTIVE"
    CAPABILITY_ID = "C01"
    CAPABILITY_VERSION = "c01-u01-1.0.0"
    CAPABILITY_SET_VERSION = "aidoctor-v1-u01"
    SCOPE_VERSION = "aidoctor-v1-scope"
    CONTRACT_VERSION = "contracts-v1"

    OTHER_RELATIONS = (
        "妈妈", "母亲", "我妈", "爸爸", "父亲", "我爸",
        "孩子", "儿子", "女儿", "老公", "丈夫", "老婆", "妻子",
        "朋友", "家人", "奶奶", "爷爷", "外婆", "外公",
    )
    SELF_MARKERS = ("我自己", "本人", "自己", "我")
    OUTSIDE_MARKERS = (
        "长期慢病管理", "长期治疗方案", "制定治疗方案", "开处方", "开药",
        "减肥计划", "健身计划", "饮食计划", "美容", "保健方案",
    )
    EXAM_MARKERS = ("检查", "化验", "检验", "报告", "影像", "CT", "核磁", "MRI", "B超")
    CLINICAL_CUES = (
        "疼", "痛", "发热", "发烧", "头晕", "咳", "恶心", "呕吐", "腹泻",
        "乏力", "不舒服", "心悸", "气短", "呼吸困难", "出血", "抽搐",
    )
    EARLY_SAFETY_CLUES = (
        "胸痛", "呼吸困难", "喘不上气", "意识不清", "昏迷", "大出血",
        "抽搐", "紫绀", "突发偏瘫", "言语不清", "剧烈头痛", "自杀", "轻生",
    )

    def __init__(self):
        self._legacy_parsing = ClinicalParsingService()

    async def interpret(self, request: C01U01Request) -> C01U01Result:
        binding = self._binding_ref()
        if not self._binding_matches(request):
            return self._insufficient_result(
                binding=binding,
                reason="CAPABILITY_BINDING_MISMATCH",
                retryable=False,
            )

        text = (request.text or "").strip()
        if not text:
            return self._insufficient_result(
                binding=binding,
                reason="EMPTY_USER_INPUT",
                retryable=False,
            )

        parsing = await self._legacy_parsing.parse(ClinicalParsingRequest(
            userId=request.userId,
            sessionId=request.consultationId or "u01-c01-pre-consultation",
            text=text,
            cdpId=None,
            input=None,
        ))

        subject = self._subject_candidate(text)
        scope = self._scope_candidate(text, parsing)
        problem = self._problem_candidate(text, parsing, scope)
        safety = self._early_safety_candidate(text)

        ambiguous = bool(parsing.ambiguousExpressions)
        if ambiguous:
            subject.uncertain = subject.uncertain or subject.subjectType == "UNKNOWN"
            scope.uncertain = True
            problem.uncertain = True

        business_status = "SUCCESS"
        reason_code = "CANDIDATES_EXTRACTED"
        if subject.subjectType == "UNKNOWN" or scope.scope in ("UNKNOWN", "MIXED") or not problem.text:
            business_status = "INSUFFICIENT_INFORMATION"
            reason_code = "CANDIDATES_REQUIRE_BUSINESS_CLARIFICATION"

        provenance = [
            "clinical-parsing-service:concept_recognizer",
            "clinical-parsing-service:ambiguity_detector",
            "c01-u01:subject-scope-safety-adapter",
        ]

        return C01U01Result(
            businessStatus=business_status,
            reasonCode=reason_code,
            retryable=False,
            bindingRef=binding,
            subjectCandidate=subject,
            problemCandidate=problem,
            scopeCandidate=scope,
            earlySafetySignalCandidate=safety,
            sourceAttribution="USER_TEXT",
            provenance=provenance,
        )

    def _binding_matches(self, request: C01U01Request) -> bool:
        return (
            request.bindingId == self.BINDING_ID
            and request.capabilitySetVersion == self.CAPABILITY_SET_VERSION
            and request.scopeVersion == self.SCOPE_VERSION
            and request.contractVersion == self.CONTRACT_VERSION
        )

    def _binding_ref(self) -> C01BindingRef:
        return C01BindingRef(
            bindingId=self.BINDING_ID,
            bindingStatus=self.BINDING_STATUS,
            capabilityId=self.CAPABILITY_ID,
            capabilityVersion=self.CAPABILITY_VERSION,
            capabilitySetVersion=self.CAPABILITY_SET_VERSION,
            scopeVersion=self.SCOPE_VERSION,
            contractVersion=self.CONTRACT_VERSION,
        )

    def _subject_candidate(self, text: str) -> SubjectCandidate:
        relation, span = self._first_match(text, self.OTHER_RELATIONS)
        if relation:
            return SubjectCandidate(
                subjectType="OTHER",
                relationText=relation,
                confidence=0.95,
                uncertain=False,
                evidenceSpans=[span],
            )

        marker, span = self._first_match(text, self.SELF_MARKERS)
        if marker:
            return SubjectCandidate(
                subjectType="SELF",
                relationText=None,
                confidence=0.90,
                uncertain=False,
                evidenceSpans=[span],
            )

        return SubjectCandidate(
            subjectType="UNKNOWN",
            relationText=None,
            confidence=0.0,
            uncertain=True,
            evidenceSpans=[],
        )

    def _problem_candidate(self(self, text, parsing, scope):
        pass
