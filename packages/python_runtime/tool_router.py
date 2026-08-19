"""显式静态 Tool 路由；无插件发现、无入口点扫描、无反射自动注册。"""

from __future__ import annotations

from collections.abc import Sequence
from dataclasses import dataclass

from aidoctor_shared_contracts import ContractEnvelope, ToolContext, ToolResult

from .artifacts import ResolvedArtifact
from .ports import ArtifactPort, ContextToolPort, ToolPort

ERROR_TOOL_UNREGISTERED = "TOOL_UNREGISTERED"
ERROR_INVOCATION_SHAPE_MISMATCH = "INVOCATION_SHAPE_MISMATCH"
ERROR_ARTIFACT_PORT_ABSENT = "ARTIFACT_PORT_ABSENT"
ERROR_INPUT_REF_UNSUPPORTED = "INPUT_REF_UNSUPPORTED"

# 仅测试注入使用；默认 create_app() 不得授权或注册该能力
ARTIFACT_PROBE_CAPABILITY_ID = "engineering.synthetic.artifact_probe"
ARTIFACT_PROBE_CAPABILITY_VERSION = "0.0.1"

_PROBE_STARTED_AT = "2026-08-19T00:00:00Z"
_PROBE_COMPLETED_AT = "2026-08-19T00:00:01Z"


class ToolRoutingError(Exception):
    """路由失败关闭；不是临床决策，也不是重试循环信号。"""

    def __init__(self, error_code: str, message: str) -> None:
        super().__init__(message)
        self.error_code = error_code
        self.message = message


@dataclass(frozen=True)
class _EnvelopeBinding:
    """信封-only 工具的显式绑定。"""

    capability_id: str
    tool_port: ToolPort


@dataclass(frozen=True)
class _ContextBinding:
    """输入承载工具的显式绑定。"""

    capability_id: str
    capability_version: str
    context_tool_port: ContextToolPort
    require_artifact: bool
    artifact_only: bool


class ToolRouter:
    """静态注册表：信封路径与 ToolContext 路径必须分开，不得混用端口。"""

    def __init__(self) -> None:
        self._envelope_bindings: dict[str, _EnvelopeBinding] = {}
        self._context_bindings: dict[str, _ContextBinding] = {}

    def register_envelope_tool(self, capability_id: str, tool_port: ToolPort) -> None:
        """显式注册仅信封工具；调用方必须传入已构造实例。"""

        self._envelope_bindings[capability_id] = _EnvelopeBinding(capability_id, tool_port)

    def register_context_tool(
        self,
        capability_id: str,
        capability_version: str,
        context_tool_port: ContextToolPort,
        *,
        require_artifact: bool = True,
        artifact_only: bool = True,
    ) -> None:
        """显式注册输入承载工具；禁止动态 import 或入口点扫描。"""

        self._context_bindings[capability_id] = _ContextBinding(
            capability_id=capability_id,
            capability_version=capability_version,
            context_tool_port=context_tool_port,
            require_artifact=require_artifact,
            artifact_only=artifact_only,
        )

    def invoke_envelope(self, envelope: ContractEnvelope) -> ToolResult:
        """按信封能力路由到 ToolPort；形状不匹配则失败关闭。"""

        binding = self._envelope_bindings.get(envelope.capability_id)
        if binding is None:
            if envelope.capability_id in self._context_bindings:
                raise ToolRoutingError(
                    ERROR_INVOCATION_SHAPE_MISMATCH,
                    "capability requires ToolContext invocation",
                )
            raise ToolRoutingError(ERROR_TOOL_UNREGISTERED, "capability is not registered")
        return binding.tool_port.invoke(envelope)

    def invoke_context(
        self,
        context: ToolContext,
        artifact_port: ArtifactPort | None,
    ) -> ToolResult:
        """按 ToolContext 能力路由，解析 ARTIFACT 后交给 ContextToolPort。"""

        capability_id = context.envelope.capability_id
        binding = self._context_bindings.get(capability_id)
        if binding is None:
            if capability_id in self._envelope_bindings:
                raise ToolRoutingError(
                    ERROR_INVOCATION_SHAPE_MISMATCH,
                    "capability requires ContractEnvelope invocation",
                )
            raise ToolRoutingError(ERROR_TOOL_UNREGISTERED, "capability is not registered")
        if binding.capability_version != context.envelope.capability_version:
            raise ToolRoutingError(
                ERROR_INVOCATION_SHAPE_MISMATCH,
                "registered capability version does not match envelope",
            )
        if artifact_port is None:
            raise ToolRoutingError(
                ERROR_ARTIFACT_PORT_ABSENT,
                "ArtifactPort is required for input-bearing invocation",
            )

        input_refs = list(context.input_refs)
        if binding.artifact_only and any(item.ref_type != "ARTIFACT" for item in input_refs):
            raise ToolRoutingError(
                ERROR_INPUT_REF_UNSUPPORTED,
                "only ARTIFACT input refs are accepted for this tool",
            )
        artifact_refs = [item for item in input_refs if item.ref_type == "ARTIFACT"]
        if binding.require_artifact and not artifact_refs:
            raise ToolRoutingError(
                ERROR_INPUT_REF_UNSUPPORTED,
                "ARTIFACT input ref is required",
            )

        resolved_artifacts: list[ResolvedArtifact] = []
        for artifact_ref in artifact_refs:
            resolved_artifacts.append(
                artifact_port.resolve(artifact_ref.ref_id, artifact_ref.ref_version)
            )
        return binding.context_tool_port.invoke(context, resolved_artifacts)


class SyntheticArtifactProbeTool:
    """合成工件探测：证明解析成功，不回传字节，不执行 OCR，不记录内容。"""

    def invoke(
        self,
        context: ToolContext,
        artifacts: Sequence[ResolvedArtifact],
    ) -> ToolResult:
        """单次确定性探测；输出仅含元数据与校验摘要。"""

        if len(artifacts) != 1:
            raise ToolRoutingError(
                ERROR_INPUT_REF_UNSUPPORTED,
                "artifact probe accepts exactly one ARTIFACT",
            )
        resolved = artifacts[0]
        first_input = context.input_refs[0]
        return ToolResult(
            contract_version=context.contract_version,
            envelope=context.envelope.model_copy(
                update={
                    "contract_name": "ToolResult",
                    "message_id": f"{context.envelope.message_id}.result",
                    "created_at": _PROBE_COMPLETED_AT,
                    "producer": "python-runtime-artifact-probe",
                }
            ),
            tool_name="synthetic-artifact-probe",
            tool_version=ARTIFACT_PROBE_CAPABILITY_VERSION,
            invocation_id=f"invoke-{context.envelope.message_id}",
            status="SUCCEEDED",
            reason_code="SYNTHETIC_ARTIFACT_PROBE_OK",
            retryable=False,
            output=[
                {"name": "artifact_id", "value": resolved.metadata.artifact_id},
                {"name": "artifact_version", "value": first_input.ref_version},
                {"name": "size_bytes", "value": resolved.metadata.size_bytes},
                {"name": "checksum_sha256", "value": resolved.metadata.checksum.value},
                {"name": "sensitivity", "value": resolved.metadata.sensitivity},
                {"name": "content_type", "value": resolved.metadata.content_type},
                {"name": "bytes_resolved", "value": True},
            ],
            suggested_patches=[],
            evidence_refs=[],
            errors=[],
            started_at=_PROBE_STARTED_AT,
            completed_at=_PROBE_COMPLETED_AT,
        )
