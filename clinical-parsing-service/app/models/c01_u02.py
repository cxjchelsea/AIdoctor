"""Typed candidate-only contract for the U02 C01 clinical fact formation slice."""
from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, Field

from app.models.c01_u01 import CapabilityBindingRef, ProvenanceStep, SourceAttribution


class ObservationLifecycle(str, Enum):
    EXTRACTED = "EXTRACTED"
    NORMALIZED = "NORMALIZED"
    CONFIRMED = "CONFIRMED"
    UNCERTAIN = "UNCERTAIN"
    CONTRADICTED = "CONTRADICTED"
    INVALIDATED = "INVALIDATED"


class FactValueSemantics(str, Enum):
    YES = "YES"
    NO = "NO"
    UNKNOWN = "UNKNOWN"
    UNMEASURED = "UNMEASURED"
    NOT_ASKED = "NOT_ASKED"
    NOT_APPLICABLE = "NOT_APPLICABLE"


class ObservationCandidate(BaseModel):
    observation_id: str
    concept_id: Optional[str] = None
    concept_display: str
    raw_text_ref: str
    normalized_value: Optional[str] = None
    value_semantics: FactValueSemantics = FactValueSemantics.UNKNOWN
    unit: Optional[str] = None
    negation: bool = False
    temporality: Optional[str] = None
    severity_or_degree: Optional[str] = None
    source_type: str = "PATIENT_REPORTED"
    lifecycle: ObservationLifecycle = ObservationLifecycle.EXTRACTED
    confidence_or_uncertainty: float = Field(ge=0.0, le=1.0)
    provenance: List[ProvenanceStep] = Field(default_factory=list)
    ambiguity_flags: List[str] = Field(default_factory=list)
    contradiction_refs: List[str] = Field(default_factory=list)


class C01U02Request(BaseModel):
    consultation_id: str
    event_id: str
    actor_id: str
    clinical_text: str
    language: str = "zh-CN"
    source_type: str = "PATIENT_REPORTED"
    binding_id: str
    capability_version: str
    capability_set_version: str
    scope_version: str
    contract_version: str


class C01U02Result(BaseModel):
    capability_call_id: str
    consultation_id: str
    event_id: str
    business_status: str
    reason_code: str
    retryable: bool = False
    binding: CapabilityBindingRef
    observations: List[ObservationCandidate] = Field(default_factory=list)
    source_attribution: SourceAttribution
    provenance: List[ProvenanceStep] = Field(default_factory=list)

    class Config:
        extra = "forbid"
