"""NC-09 ProviderAdapter Protocol (sync, PreparedInvocation-only)."""

from __future__ import annotations

from typing import Protocol, runtime_checkable

from ..gateway.models import PreparedInvocation
from .models import ModelInvocationResult


@runtime_checkable
class ProviderAdapter(Protocol):
    """Explicit provider execution boundary.

    P4 is sync-only. Implementations must not re-select models, re-render prompts,
    or approve clinical/state outcomes. Future real providers may implement this
    Protocol under separate authorization; P4 only ships DeterministicFakeProviderAdapter.
    """

    @property
    def provider_id(self) -> str:
        """Stable provider identity validated by Identifier authority."""

    def invoke(self, prepared: PreparedInvocation) -> ModelInvocationResult:
        """Execute against one PreparedInvocation and return a candidate-only result."""
