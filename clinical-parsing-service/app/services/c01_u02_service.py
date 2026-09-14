"""C01-U02 candidate formation. This service never mutates Clinical State."""
import hashlib
import re
import uuid
from typing import Dict, List, Optional, Tuple

from app.models.c01_u01 import CapabilityBindingRef, ProvenanceStep, SourceAttribution
from app.models.c01_u02 import (
    C01U02Request,
    C01U02Result,
    FactValueSemantics,
    ObservationCandidate,
    ObservationLifecycle,
)
from app.services.concept_recognizer import ConceptRecognizer
from app.utils.vocabulary_loader import VocabularyLoader


CAPABILITY_ID = "C01"
CAPABILITY_VERSION = "c01-u02-v1"
CAPABILITY_SET_VERSION = "phase7-v1"
SCOPE_VERSION = "phase6-v1"
CONTRACT_VERSION = "phase8-v1"

_NEGATION = ("没有", "无", "否认", "未出现", "不伴", "没")
_UNKNOWN = ("不清楚", "不知道", "不确定", "记不清")
_UNMEASURED = ("没测", "未测", "没有测", "没量", "未量")
_TEMPORAL = (
    (r"(刚刚|刚才|今天)", "CURRENT"),
    (r"(昨天|前天)", "RECENT_PAST"),
    (r"(\d+)\s*(天|周|个月|月|年)(前|以来|左右)?", None),
)
_SEVERITY = (("轻微", "MILD"), ("轻度", "MILD"), ("中度", "MODERATE"), ("严重", "SEVERE"), ("剧烈", "SEVERE"))
_VALUE_UNIT = re.compile(r"(?P<value>\d+(?:\.\d+)?)\s*(?P<unit>℃|°C|mmHg|bpm|次/分|mg|g|ml|mL|mmol/L|%)")


class C01U02ClinicalUnderstandingService:
    def __init__(self) -> None:
        vocabulary = VocabularyLoader()
        self._recognizer = ConceptRecognizer(vocabulary)

    async def interpret(self, request: C01U02Request) -> C01U02Result:
        call_id = str(uuid.uuid4())
        source = SourceAttribution(
            source_type=request.source_type,
            actor_id=request.actor_id,
            event_id=request.event_id,
        )
        provenance = [ProvenanceStep(step="C01_U02_INPUT_ACCEPTED", detail="raw_text_fingerprint_only")]
        binding = CapabilityBindingRef(
            capability_id=CAPABILITY_ID,
            binding_id=request.binding_id,
            capability_version=request.capability_version,
            capability_set_version=request.capability_set_version,
            scope_version=request.scope_version,
            contract_version=request.contract_version,
        )

        if not self._binding_matches(request):
            return self._result(call_id, request, binding, source, provenance, "FAILED", "CAPABILITY_BINDING_MISMATCH", True, [])

        text = (request.clinical_text or "").strip()
        if not text:
            return self._result(call_id, request, binding, source, provenance, "INSUFFICIENT_INFORMATION", "EMPTY_CLINICAL_INPUT", False, [])

        raw_ref = "sha256:" + hashlib.sha256(text.encode("utf-8")).hexdigest()
        concepts = self._recognizer.recognize(text)
        observations = self._to_observations(text, raw_ref, concepts, request.source_type)
        if not observations:
            observations = [self._fallback_observation(text, raw_ref, request.source_type)]

        status = "SUCCEEDED"
        reason = "OBSERVATION_CANDIDATES_FORMED"
        if all(item.lifecycle == ObservationLifecycle.UNCERTAIN for item in observations):
            status = "INSUFFICIENT_INFORMATION"
            reason = "ONLY_UNCERTAIN_OBSERVATIONS"
        return self._result(call_id, request, binding, source, provenance, status, reason, False, observations)

    def _binding_matches(self, request: C01U02Request) -> bool:
        return (
            request.capability_version == CAPABILITY_VERSION
            and request.capability_set_version == CAPABILITY_SET_VERSION
            and request.scope_version == SCOPE_VERSION
            and request.contract_version == CONTRACT_VERSION
            and bool(request.binding_id)
        )

    def _to_observations(self, text: str, raw_ref: str, concepts: List[Dict], source_type: str) -> List[ObservationCandidate]:
        result: List[ObservationCandidate] = []
        seen: Dict[Tuple[str, str], ObservationCandidate] = {}
        for index, concept in enumerate(concepts):
            display = str(concept.get("standard_term") or concept.get("original_text") or "").strip()
            if not display:
                continue
            concept_id = self._concept_id(concept)
            key = (concept_id or display, display)
            if key in seen:
                continue
            candidate = self._build_candidate(index, text, raw_ref, concept_id, display, source_type, float(concept.get("confidence") or 0.5))
            seen[key] = candidate
            result.append(candidate)
        return result

    def _fallback_observation(self, text: str, raw_ref: str, source_type: str) -> ObservationCandidate:
        return self._build_candidate(0, text, raw_ref, None, "UNRESOLVED_CLINICAL_EXPRESSION", source_type, 0.25)

    def _build_candidate(self, index: int, text: str, raw_ref: str, concept_id: Optional[str], display: str, source_type: str, confidence: float) -> ObservationCandidate:
        negated = any(token in text for token in _NEGATION)
        unknown = any(token in text for token in _UNKNOWN)
        unmeasured = any(token in text for token in _UNMEASURED)
        value_match = _VALUE_UNIT.search(text)
        normalized_value = value_match.group("value") if value_match else None
        unit = value_match.group("unit") if value_match else None
        temporality = self._temporality(text)
        severity = next((mapped for token, mapped in _SEVERITY if token in text), None)
        ambiguity: List[str] = []
        semantics = FactValueSemantics.NO if negated else FactValueSemantics.YES
        lifecycle = ObservationLifecycle.NORMALIZED if concept_id else ObservationLifecycle.EXTRACTED
        effective_confidence = max(0.0, min(1.0, confidence))
        if unknown:
            semantics = FactValueSemantics.UNKNOWN
            lifecycle = ObservationLifecycle.UNCERTAIN
            ambiguity.append("USER_UNCERTAIN")
            effective_confidence = min(effective_confidence, 0.4)
        elif unmeasured:
            semantics = FactValueSemantics.UNMEASURED
            lifecycle = ObservationLifecycle.UNCERTAIN
            ambiguity.append("UNMEASURED_VALUE")
            effective_confidence = min(effective_confidence, 0.5)
        elif concept_id is None:
            semantics = FactValueSemantics.UNKNOWN
            lifecycle = ObservationLifecycle.UNCERTAIN
            ambiguity.append("UNRESOLVED_CONCEPT")

        return ObservationCandidate(
            observation_id="obs-%s-%s" % (index + 1, raw_ref[-12:]),
            concept_id=concept_id,
            concept_display=display,
            raw_text_ref=raw_ref,
            normalized_value=normalized_value,
            value_semantics=semantics,
            unit=unit,
            negation=negated,
            temporality=temporality,
            severity_or_degree=severity,
            source_type=source_type,
            lifecycle=lifecycle,
            confidence_or_uncertainty=effective_confidence,
            provenance=[ProvenanceStep(step="C01_U02_CANDIDATE_FORMED", detail="candidate_only_no_state_write")],
            ambiguity_flags=ambiguity,
            contradiction_refs=[],
        )

    def _temporality(self, text: str) -> Optional[str]:
        for pattern, mapped in _TEMPORAL:
            match = re.search(pattern, text)
            if match:
                return mapped or match.group(0)
        return None

    @staticmethod
    def _concept_id(concept: Dict) -> Optional[str]:
        for key in ("cui", "snomed", "loinc", "atc", "icd"):
            value = concept.get(key)
            if value:
                return "%s:%s" % (key.upper(), value)
        return None

    @staticmethod
    def _result(call_id, request, binding, source, provenance, status, reason, retryable, observations):
        return C01U02Result(
            capability_call_id=call_id,
            consultation_id=request.consultation_id,
            event_id=request.event_id,
            business_status=status,
            reason_code=reason,
            retryable=retryable,
            binding=binding,
            observations=observations,
            source_attribution=source,
            provenance=provenance,
        )
