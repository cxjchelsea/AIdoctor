# C01-U01 临床理解能力实施前评估

> 基线：main@31e77d686e43072096fa52ed5c891bc01c17e76a
> 范围：仅 U01 首个消费者所需的 C01 最小能力切片；不提前实现 U02 的完整 Clinical Fact parsing。

## 1. 冻结需求

Phase 7 已明确：

- C01 负责 Subject / Problem semantic extraction、Scope-related semantic extraction、Ambiguity / Contradiction Detection、Source Attribution 等候选语义；
- C01 不拥有 Clinical Facts 真值，不拥有 Scope 最终裁决；
- FIRST_CONSUMER_UNIT = U01；
- U01 最小建设范围 = Subject / Problem / Scope semantic extraction；
- 现有 Clinical Parsing + Dialog NLU + Health State Assessment = ADAPT + REFACTOR。

## 2. 当前资产评估

| 资产 | 当前能力 | 处置 |
|---|---|---|
| `clinical-parsing-service` | 概念识别、症状/疾病/药物/检查/过敏结构化、歧义检测 | ADAPT + REFACTOR |
| `ClinicalParsingClient` | `Object parse(Object)` + legacy tool_1 | LEGACY_COMPAT / BLOCK_NEW_C01_USE |
| legacy `tool_1` suggested_writes | 可建议直接写 `cdp.patient_state` | LEGACY_ONLY / BLOCK_NEW_C01_USE |
| U01 `U01SemanticPolicy` | 确定性 Subject/Problem/Scope 正式解释 | KEEP |
| U01 `U01StartCommand` | 已接收结构化 subject/problem/scope candidate | KEEP / ADAPT |

## 3. Gap

当前缺失：

1. 自然语言到 SubjectCandidate 的正式 C01 输出；
2. ProblemCandidate 与 evidence span / uncertainty；
3. ScopeCandidate，尤其 MIXED/UNKNOWN 不得静默压平；
4. early safety clue 的保守候选透传；
5. 一等 Failure 语义；
6. capability/scope/contract binding metadata；
7. U01 natural-language adapter/gateway；
8. Capability EvalSet。

## 4. 本切片决定

本切片新增独立 C01-U01 typed capability contract，不复用 legacy `tool_1` `suggested_writes` 作为正式路径。

正式链：

```text
raw user input
→ C01-U01 candidate extraction
→ typed Capability Result
→ U01 Business Owner / D10
→ U01 committed Consultation state
```

保持：

```text
Capability Result != Clinical Truth
ScopeCandidate != ScopeDecision
EarlySafetySignalCandidate != Clinical Risk
Capability failure != clarification / safe / negative
```

## 5. 最小验收

- SELF / OTHER / UNKNOWN subject candidate；
- OTHER 关系词不会伪造 subjectReferenceId；
- clinical / outside / mixed / unknown scope candidate；
- 混合诉求不掩盖临床不适；
- 空输入/无语义输入不得静默猜测；
- early safety clue 只作为 candidate 透传；
- response 包含 binding/provenance/source/uncertainty/failure 语义；
- U01 通过 C01 candidate 后仍由 `U01SemanticPolicy` 做正式裁决；
- canonical replay 不产生第二次 U01 业务 effect；
- 新 C01 路径不得依赖 legacy `tool_1` direct-write 语义。
