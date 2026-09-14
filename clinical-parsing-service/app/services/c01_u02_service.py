"""C01-U02 candidate formation. This service never mutates Clinical State."""
import hashlib
import re
from typing import Dict, List, Optional

from app.config.settings import settings
from app.models.c01_u01 import C01BindingRef
from app.models.c01_u02 import C01U02Request, C01U02Result, ObservationCandidate
from app.services.ambiguity_detector import AmbiguityDetector
from app.services.concept_recognizer import ConceptRecognizer
from app.utils.vocabulary_loader import VocabularyLoader


class C01U02ClinicalUnderstandingService:
    BINDING_ID = "c01-u02-v1-active"
    BINDING_STATUS = "ACTIVE"
    CAPABILITY_ID = "C01"
    CAPABILITY_VERSION = "c01-u02-1.0.0"
    CAPABILITY_SET_VERSION = "aidoctor-v1-u02"
    SCOPE_VERSION = "aidoctor-v1-scope"
    CONTRACT_VERSION = "contracts-v1"

    NEGATION_MARKERS = ("没有", "无", "否认", "未出现", "不伴", "没")
    UNKNOWN_MARKERS = ("不清楚", "不知道", "不确定", "记不清")
    UNMEASURED_MARKERS = ("没测", "未测", "没有测", "没量", "未量")
    SEVERITY = (("轻微", "MILD"), ("轻度", "MILD"), ("中度", "MODERATE"), ("严重", "SEVERE"), ("剧烈", "SEVERE"))
    VALUE_UNIT = re.compile(r"(?P<value>\d+(?:\.\d+)?)\s*(?P<unit>℃|°C|mmHg|bpm|次/分|mg|g|ml|mL|mmol/L|%)")
    CLAUSE_SEPARATORS = ",，。;；!?！？\n"

    def __init__(self):
        vocabulary_loader = VocabularyLoader(settings.vocabulary_base_path)
        vocabulary_loader.load_all_vocabularies()
        self._concept_recognizer = ConceptRecognizer(vocabulary_loader)
        self._ambiguity_detector = AmbiguityDetector()

    async def interpret(self, request: C01U02Request) -> C01U02Result:
        binding = self._binding_ref()
        if not self._binding_matches(request):
            return self._result(binding, "UNSUPPORTED", "CAPABILITY_BINDING_MISMATCH", False, [])
        text = (request.text or "").strip()
        if not text:
            return self._result(binding, "INSUFFICIENT_INFORMATION", "EMPTY_CLINICAL_INPUT", False, [])

        concepts = self._concept_recognizer.recognize(text)
        ambiguities = self._ambiguity_detector.detect(text, concepts)
        raw_ref = "sha256:" + hashlib.sha256(text.encode("utf-8")).hexdigest()
        candidates = self._candidates(text, raw_ref, concepts, request.sourceType, bool(ambiguities))
        if not candidates:
            candidates = [self._candidate(0, text, raw_ref, None, "UNRESOLVED_CLINICAL_EXPRESSION", request.sourceType, 0.25, True)]

        uncertain_only = all(item.lifecycle == "UNCERTAIN" for item in candidates)
        return self._result(
            binding,
            "INSUFFICIENT_INFORMATION" if uncertain_only else "SUCCESS",
            "ONLY_UNCERTAIN_OBSERVATIONS" if uncertain_only else "OBSERVATION_CANDIDATES_EXTRACTED",
            False,
            candidates,
        )

    def _binding_matches(self, request: C01U02Request) -> bool:
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

    def _candidates(self, text: str, raw_ref: str, concepts: List[Dict], source_type: str, ambiguous: bool) -> List[ObservationCandidate]:
        result = []
        seen = set()
        for index, concept in enumerate(concepts):
            display = str(concept.get("standard_term") or concept.get("original_text") or "").strip()
            if not display:
                continue
            concept_id = self._concept_id(concept)
            key = (concept_id or display, display)
            if key in seen:
                continue
            seen.add(key)
            local_text = self._clause_for_concept(text, concept, display)
            result.append(self._candidate(
                index,
                local_text,
                raw_ref,
                concept_id,
                display,
                source_type,
                float(concept.get("confidence") or 0.5),
                ambiguous,
            ))
        return result

    def _candidate(self, index, text, raw_ref, concept_id, display, source_type, confidence, ambiguous):
        # Negation/value/temporality/severity are scoped to this concept's local
        # clause, never copied from an unrelated concept elsewhere in the input.
        negated = any(marker in text for marker in self.NEGATION_MARKERS)
        unknown = any(marker in text for marker in self.UNKNOWN_MARKERS)
        unmeasured = any(marker in text for marker in self.UNMEASURED_MARKERS)
        value_match = self.VALUE_UNIT.search(text)
        flags = ["AMBIGUOUS_EXPRESSION"] if ambiguous else []
        value_semantics = "NO" if negated else "YES"
        lifecycle = "NORMALIZED" if concept_id else "EXTRACTED"
        effective_confidence = max(0.0, min(1.0, confidence))
        if unknown:
            value_semantics, lifecycle = "UNKNOWN", "UNCERTAIN"
            flags.append("USER_UNCERTAIN")
            effective_confidence = min(effective_confidence, 0.4)
        elif unmeasured:
            value_semantics, lifecycle = "UNMEASURED", "UNCERTAIN"
            flags.append("UNMEASURED_VALUE")
            effective_confidence = min(effective_confidence, 0.5)
        elif concept_id is None:
            value_semantics, lifecycle = "UNKNOWN", "UNCERTAIN"
            flags.append("UNRESOLVED_CONCEPT")
        return ObservationCandidate(
            observationId="obs-%s-%s" % (index + 1, raw_ref[-12:]),
            conceptId=concept_id,
            conceptDisplay=display,
            rawTextRef=raw_ref,
            normalizedValue=value_match.group("value") if value_match else None,
            valueSemantics=value_semantics,
            unit=value_match.group("unit") if value_match else None,
            negation=negated,
            temporality=self._temporality(text),
            severityOrDegree=next((mapped for marker, mapped in self.SEVERITY if marker in text), None),
            sourceType=source_type,
            lifecycle=lifecycle,
            confidenceOrUncertainty=effective_confidence,
            provenance=["c01-u02:observation-candidate-only"],
            ambiguityFlags=flags,
            contradictionRefs=[],
        )

    @classmethod
    def _clause_for_concept(cls, text: str, concept: Dict, display: str) -> str:
        needle = str(concept.get("original_text") or display or "").strip()
        if not needle:
            return text
        position = text.find(needle)
        if position < 0 and display:
            position = text.find(display)
        if position < 0:
            return text

        left = 0
        right = len(text)
        for separator in cls.CLAUSE_SEPARATORS:
            found = text.rfind(separator, 0, position)
            if found >= left:
                left = found + 1
            found = text.find(separator, position + len(needle))
            if found >= 0:
                right = min(right, found)
        return text[left:right].strip()

    @staticmethod
    def _concept_id(concept: Dict) -> Optional[str]:
        for key in ("cui", "snomed", "loinc", "atc", "icd"):
            if concept.get(key):
                return "%s:%s" % (key.upper(), concept[key])
        return None

    @staticmethod
    def _temporality(text: str) -> Optional[str]:
        if any(marker in text for marker in ("刚刚", "刚才", "今天")):
            return "CURRENT"
        if any(marker in text for marker in ("昨天", "前天")):
            return "RECENT_PAST"
        match = re.search(r"\d+\s*(天|周|个月|月|年)(前|以来|左右)?", text)
        return match.group(0) if match else None

    @staticmethod
    def _result(binding, status, reason, retryable, candidates):
        return C01U02Result(
            businessStatus=status,
            reasonCode=reason,
            retryable=retryable,
            bindingRef=binding,
            observationCandidates=candidates,
            sourceAttribution="USER_TEXT",
            provenance=["clinical-parsing-service:concept_recognizer", "clinical-parsing-service:ambiguity_detector", "c01-u02:observation-adapter"],
        )
