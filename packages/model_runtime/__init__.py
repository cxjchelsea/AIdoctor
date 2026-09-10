"""Non-clinical structural contracts for the governed model runtime."""

from .api import ModelSpec, PromptSpec
from .gateway import GatewayRequest, ModelGateway, PreparedInvocation
from .prompts import PromptBuilder, PromptDocument, PromptLoader, PromptRegistry
from .providers import (
    DeterministicFakeProviderAdapter,
    ModelInvocationResult,
    ProviderAdapter,
    ProviderAdapterError,
    ProviderAdapterErrorCode,
    ProviderInvocationOutcome,
)
from .routing import ModelRoutePolicy
from .schemas import OutputSchemaRegistry, SharedContractValidator

__all__ = [
    "DeterministicFakeProviderAdapter",
    "GatewayRequest",
    "ModelGateway",
    "ModelInvocationResult",
    "ModelRoutePolicy",
    "ModelSpec",
    "OutputSchemaRegistry",
    "PreparedInvocation",
    "PromptBuilder",
    "PromptDocument",
    "PromptLoader",
    "PromptRegistry",
    "PromptSpec",
    "ProviderAdapter",
    "ProviderAdapterError",
    "ProviderAdapterErrorCode",
    "ProviderInvocationOutcome",
    "SharedContractValidator",
]
