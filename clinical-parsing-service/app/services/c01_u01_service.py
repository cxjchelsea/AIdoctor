"""C01 minimal clinical-understanding slice for U01.

The service produces candidates only. Final Subject/Problem/Scope decisions remain
owned by U01/D10. This adapter deliberately does not produce CDP writes.
"""
from typing import Iterable, Optional, Tuple

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
        "我妈妈", "妈妈", "母亲", "我妈", "我爸爸", "爸爸", "父亲", "我爸",
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
        # Reuse existing concept recognition and ambiguity assets; do not reuse legacy
        # tool_1 write suggestions or CDP mutation semantics.
        self._legacy_parsing = ClinicalParsingService()

    async def interpret(self, request: C01U01Request) -> C01U01Result:
        binding = self._binding_ref()
        if not self._binding_matches(request):
            return self._insufficient_result(
                binding=binding,
                reason="CAPABILITY_BINDING_MISMATCH",
                retryable=False,
                status="UNSUPPORTED",
            )

        text = (request.text or "").strip()
        if not text:
            return self._insufficient_result(
                binding=binding,
                reason="EMPTY_USER_INPUT",
                retryable=False,
                status="INSUFFICIENT_INFORMATION",
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
        if relation and span:
            return SubjectCandidate(
                subjectType="OTHER",
                relationText=relation,
                confidence=0.95,
                uncertain=False,
                evidenceSpans=[span],
            )

        marker, span = self._first_match(text, self.SELF_MARKERS)
        if marker and span:
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

    def _problem_candidate(self, text, parsing, scope: ScopeCandidate) -> ProblemCandidate:
        semantic_spans = []
        seen = set()
        for concept in parsing.concepts:
            original = (concept.originalText or "").strip()
            if original and original not in seen:
                span = self._span(text, original)
                if span:
                    semantic_spans.append(span)
                    seen.add(original)

        if not semantic_spans:
            for marker in self.CLINICAL_CUES + self.EXAM_MARKERS + self.OUTSIDE_MARKERS:
                span = self._span(text, marker)
                if span and marker not in seen:
                    semantic_spans.append(span)
                    seen.add(marker)

        if scope.scope == "UNKNOWN" and not semantic_spans:
            return ProblemCandidate(
                text=None,
                confidence=0.0,
                uncertain=True,
                evidenceSpans=[],
            )

        return ProblemCandidate(
            text=text,
            confidence=0.85 if semantic_spans else 0.60,
            uncertain=scope.uncertain,
            evidenceSpans=semantic_spans,
        )

    def _scope_candidate(self, text, parsing) -> ScopeCandidate:
        outside_spans = self._all_matches(text, self.OUTSIDE_MARKERS)
        exam_spans = self._all_matches(text, self.EXAM_MARKERS)
        clinical_spans = self._all_matches(text, self.CLINICAL_CUES)

        for concept in parsing.concepts:
            original = (concept.originalText or "").strip()
            span = self._span(text, original) if original else None
            if span:
                clinical_spans.append(span)

        if parsing.structuredData.examinations:
            for exam in parsing.structuredData.examinations:
                span = self._span(text, exam.name)
                if span:
                    exam_spans.append(span)

        outside = bool(outside_spans)
        exam = bool(exam_spans)
        clinical = bool(clinical_spans) or bool(parsing.structuredData.symptoms)

        if outside and (clinical or exam):
            return ScopeCandidate(
                scope="MIXED",
                confidence=0.90,
                uncertain=True,
                evidenceSpans=self._dedupe_spans(outside_spans + exam_spans + clinical_spans),
            )
        if exam:
            return ScopeCandidate(
                scope="EXAMINATION",
                confidence=0.90,
                uncertain=False,
                evidenceSpans=self._dedupe_spans(exam_spans + clinical_spans),
            )
        if clinical:
            return ScopeCandidate(
                scope="SYMPTOM",
                confidence=0.88,
                uncertain=False,
                evidenceSpans=self._dedupe_spans(clinical_spans),
            )
        if outside:
            return ScopeCandidate(
                scope="OUTSIDE_V1_INTENT",
                confidence=0.92,
                uncertain=False,
                evidenceSpans=self._dedupe_spans(outside_spans),
            )
        return ScopeCandidate(
            scope="UNKNOWN",
            confidence=0.0,
            uncertain=True,
            evidenceSpans=[],
        )

    def _early_safety_candidate(self, text: str) -> EarlySafetySignalCandidate:
        spans = self._all_matches(text, self.EARLY_SAFETY_CLUES)
        clues = [span.text for span in spans]
        return EarlySafetySignalCandidate(
            detected=bool(spans),
            clues=clues,
            evidenceSpans=self._dedupe_spans(spans),
        )

    def _insufficient_result(
        self,
        binding: C01BindingRef,
        reason: str,
        retryable: bool,
        status: str,
    ) -> C01U01Result:
        return C01U01Result(
            businessStatus=status,
            reasonCode=reason,
            retryable=retryable,
            bindingRef=binding,
            subjectCandidate=SubjectCandidate(
                subjectType="UNKNOWN", confidence=0.0, uncertain=True, evidenceSpans=[]
            ),
            problemCandidate=ProblemCandidate(
                text=None, confidence=0.0, uncertain=True, evidenceSpans=[]
            ),
            scopeCandidate=ScopeCandidate(
                scope="UNKNOWN", confidence=0.0, uncertain=True, evidenceSpans=[]
            ),
            earlySafetySignalCandidate=EarlySafetySignalCandidate(
                detected=False, clues=[], evidenceSpans=[]
            ),
            sourceAttribution="USER_TEXT",
            provenance=["c01-u01:binding-validation"],
        )

    def _first_match(
        self, text: str, markers: Iterable[str]
    ) -> Tuple[Optional[str], Optional[EvidenceSpan]]:
        for marker in markers:
            span = self._span(text, marker)
            if span:
                return marker, span
        return None, None

    def _all_matches(self, text: str, markers: Iterable[str]):
        spans = []
        for marker in markers:
            start = text.find(marker)
            while start >= 0:
                spans.append(EvidenceSpan(
                    text=marker,
                    start=start,
                    end=start + len(marker),
                    source="USER_TEXT",
                ))
                start = text.find(marker, start + len(marker))
        return self._dedupe_spans(spans)

    def _span(self, text: str, marker: str) -> Optional[EvidenceSpan]:
        start = text.find(marker)
        if start < 0:
            return None
        return EvidenceSpan(
            text=marker,
            start=start,
            end=start + len(marker),
            source="USER_TEXT",
        )

    @staticmethod
    def _dedupe_spans(spans):
        seen = set()
        result = []
        for span in spans:
            key = (span.start, span.end, span.text)
            if key not in seen:
                seen.add(key)
                result.append(span)
        return result
