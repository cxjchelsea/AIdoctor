"""执行态 Checkpoint 接口与测试专用内存实现。

分类：TEST_ONLY / NON_PRODUCTION / EXECUTION_METADATA_ONLY
checkpoint != 临床权威；不得写入 SoR，不得实现 State Committer。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, Optional


@dataclass(frozen=True)
class CheckpointRecord:
    """仅含合成 Runtime 测试所需的执行元数据。"""

    run_id: str
    step_name: str
    status: str


class InMemoryCheckpointPort:
    """TEST_ONLY / NON_PRODUCTION / EXECUTION_METADATA_ONLY 内存适配器。"""

    def __init__(self) -> None:
        # 仅保存执行元数据，不做持久化 vendor 选择
        self._records: Dict[str, CheckpointRecord] = {}

    def save(self, run_id: str, record: CheckpointRecord) -> None:
        """按 run_id 覆盖写入一条执行记录。"""

        if record.run_id != run_id:
            raise ValueError("checkpoint run_id must match the save key")
        self._records[run_id] = record

    def load(self, run_id: str) -> Optional[CheckpointRecord]:
        """读取执行记录；缺失返回 None。"""

        return self._records.get(run_id)
