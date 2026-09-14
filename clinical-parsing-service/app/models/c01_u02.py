"""Typed candidate-only contract for the U02 C01 clinical fact formation slice.

This contract extends the existing C01 convention. It carries Observation
Candidates only and never represents committed Clinical Truth or K09 Proposal.
"""
from typing import List, Optional

from pydantic import BaseModel, Field

from app.models.c01_u01 import C01BindingRef


class ObservationCandidate(BaseModel):
    observationId: str
    conceptId: Optional[str] = None
    conceptDisplay: str
    rawTextRef: str
    normalizedValue: Optional[str] = None
    valueSemantics: str
    unit: Optional[str] = None
    negation: bool = False
    temporality: Optional[str] = None
    severityOrDegree: Optional[str] = None
    sourceType: str = "PATIENT_REPORTED"
    lifecycle: str = "EXTRACTED"
    confidenceOrUncertainty: float = Field(ge=0.0, le=1.0)
    provenance: List[str] = Field(default_factory=list)
    ambiguityFlags: List[str] = Field(default_factory=list)
    contradictionRefs: List[str] = Field(default_factory=list)


class C01U02Request(BaseModel):
    userId: str
    text: str
    consultationId: str
    eventId: str
    sourceType: str = "PATIENT_REPORTED"
    bindingId: str
    capabilitySetVersion: str
    scopeVersion: str
    contractVersion: str


class C01U02Result(BaseModel):
    businessStatus: str
    reasonCode: str
    retryable: bool
    bindingRef: C01BindingRef
    observationCandidates: List[ObservationCandidate] = Field(default_factory=list)
    sourceAttribution: str = "USER_TEXT"
    provenance: List[str] = Field(default_factory=list)

    class Config:
        extra = "forbid"
