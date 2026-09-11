"""Typed C01/U01 capability contract.

This contract carries candidates only. It must never be interpreted as committed
Clinical Truth or a K09 StateChangeProposal.
"""
from typing import List, Optional
from pydantic import BaseModel, Field


class C01BindingRef(BaseModel):
    bindingId: str
    bindingStatus: str
    capabilityId: str
    capabilityVersion: str
    capabilitySetVersion: str
    scopeVersion: str
    contractVersion: str


class EvidenceSpan(BaseModel):
    text: str
    start: int
    end: int
    source: str = "USER_TEXT"


class SubjectCandidate(BaseModel):
    subjectType: str
    relationText: Optional[str] = None
    confidence: float
    uncertain: bool
    evidenceSpans: List[EvidenceSpan] = Field(default_factory=list)


class ProblemCandidate(BaseModel):
    text: Optional[str] = None
    confidence: float
    uncertain: bool
    evidenceSpans: List[EvidenceSpan] = Field(default_factory=list)


class ScopeCandidate(BaseModel):
    scope: str
    confidence: float
    uncertain: bool
    evidenceSpans: List[EvidenceSpan] = Field(default_factory=list)


class EarlySafetySignalCandidate(BaseModel):
    detected: bool
    clues: List[str] = Field(default_factory=list)
    evidenceSpans: List[EvidenceSpan] = Field(default_factory=list)


class C01U01Request(BaseModel):
    userId: str
    text: str
    consultationId: Optional[str] = None
    knownSubjectReferenceId: Optional[str] = None
    bindingId: str
    capabilitySetVersion: str
    scopeVersion: str
    contractVersion: str


class C01U01Result(BaseModel):
    businessStatus: str
    reasonCode: str
    retryable: bool
    bindingRef: C01BindingRef
    subjectCandidate: SubjectCandidate
    problemCandidate: ProblemCandidate
    scopeCandidate: ScopeCandidate
    earlySafetySignalCandidate: EarlySafetySignalCandidate
    sourceAttribution: str = "USER_TEXT"
    provenance: List[str] = Field(default_factory=list)
