"""NC-CLOSE-03 临床阻断结构断言。

只检查仓库内机器可读状态，确认 adult_respiratory_v1 仍处于未启用 /
不可生产的治理状态。

本测试不执行诊断、不加载临床 gold、不批准 Prompt、不评价医学内容、
不调用 provider、不使用 PHI。CI 失败只表示结构治理状态被静默改写，
不表示临床对错。
"""

from __future__ import annotations

from pathlib import Path

import yaml

# 权威机器可读来源：Capability 清单，而不是文档叙述。
MANIFEST_PATH = (
    Path(__file__).resolve().parents[1] / "adult_respiratory_v1" / "manifest.yaml"
)

# 当前冻结基线要求的阻断字段。字段名必须与清单完全一致，不得臆造。
REQUIRED_CLINICAL_BLOCK_STATE = {
    "lifecycle": "DRAFT",
    "clinical_review_status": "REQUIRES_CLINICAL_REVIEW",
    "runtime_adoption": "NOT_IMPLEMENTED",
    "production_eligibility": "BLOCKED",
}


def test_adult_respiratory_v1_remains_structurally_blocked():
    """未来 PR 若把权威清单改成未授权的启用/可生产状态，本断言必须失败。"""
    manifest = yaml.safe_load(MANIFEST_PATH.read_text(encoding="utf-8"))

    assert manifest["capability_id"] == "adult_respiratory_v1"
    for field_name, expected_value in REQUIRED_CLINICAL_BLOCK_STATE.items():
        assert manifest[field_name] == expected_value, (
            f"{MANIFEST_PATH.as_posix()} {field_name} must remain {expected_value!r}; "
            f"got {manifest.get(field_name)!r}"
        )
