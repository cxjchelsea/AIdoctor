"""确定性 FakeToolPort：不包装遗留服务，不产生临床建议。"""

from __future__ import annotations

from aidoctor_shared_contracts import ContractEnvelope, ToolResult

from .protocol import validate_envelope

# 合成时间戳：固定字符串，避免时钟与患者语义
_SYNTHETIC_STARTED_AT = "2026-08-18T00:00:00Z"
_SYNTHETIC_COMPLETED_AT = "2026-08-18T00:00:01Z"


class FakeToolPort:
    """测试用确定性工具端口；输出不含临床推荐。"""

    def invoke(self, envelope: ContractEnvelope) -> ToolResult:
        """由信封字段派生稳定 ToolResult，相同输入得到相同逻辑结果。"""

        validated = validate_envelope(envelope)
        return ToolResult(
            contract_version=validated.contract_version,
            envelope=validated.model_copy(
                update={
                    "contract_name": "ToolResult",
                    "message_id": f"{validated.message_id}.result",
                    "created_at": _SYNTHETIC_COMPLETED_AT,
                    "producer": "python-runtime-smoke",
                }
            ),
            tool_name="synthetic-echo",
            tool_version="1.0.0",
            invocation_id=f"invoke-{validated.message_id}",
            status="SUCCEEDED",
            reason_code="SYNTHETIC_RUNTIME_OK",
            retryable=False,
            output=[{"name": "runtime_status", "value": "ok"}],
            suggested_patches=[],
            evidence_refs=[],
            errors=[],
            started_at=_SYNTHETIC_STARTED_AT,
            completed_at=_SYNTHETIC_COMPLETED_AT,
        )
