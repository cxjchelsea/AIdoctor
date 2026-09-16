# U03 C / D / E v0.2.1 Evaluation Reuse Assessment

> 对象：`RR-U03-RISK-001@0.2.1-draft`、`PR-U03-D09-001@0.2.1-draft`、`U03_D09_COVERAGE_V0_2_1_DRAFT` 冻结前 evaluation reuse 判断。  
> 状态：`ASSESSMENT_COMPLETE / BULK_REUSE_ALLOWED / TARGETED_SCOPE_DELTA_REQUIRED / NEW_CANDIDATES_BLOCKED / NOT_GATE_C / NOT_FOR_PRODUCTION`  
> 本文件不创建 candidate、不冻结、不修改历史 0.2.0 candidate，不构成 Gate B / Gate C / Implementation Authorization。

---

## 1. Inputs

```text
U03_CDE_Scope_Alignment_Targeted_Review_Record_v0.2.1.md
U03_C_Rule_Release_Scope_Revision_Draft_v0.2.1.md
U03_D09_Policy_Scope_Revision_Draft_v0.2.1.md
U03_D09_Coverage_Contract_Revision_Draft_v0.2.1.md
U03_C_PreFreeze_Evaluation_Fixtures_v0.2.md          # 57 fixtures
U03_D09_PreFreeze_Evaluation_Fixtures_v0.1.md        # 48 fixtures
```

已确认 targeted review：

```text
C 0.2.1 Medical / Technical = APPROVE / APPROVE
D 0.2.1 Medical / Technical = APPROVE / APPROVE
Coverage 0.2.1 Medical / Technical = APPROVE / APPROVE
CDE-SCOPE-X1..X4 = APPROVE / APPROVE
BF-CDE-01 = CLOSED_FOR_CONTENT
```

---

## 2. Delta Classification

0.2.1 唯一内容 delta：

```text
pregnancy / puerperium
从“仅 NG253 sepsis family exclusion”
提升为
“current U03 whole-policy slice exclusion”
```

保持不变：

```text
C active rules = 15 unchanged
C predicates / thresholds = unchanged
C RULE_SIGNAL_* vocabulary = unchanged
PF-U03-C-POLICY-001 internal missingness/sepsis semantics = unchanged

D branches = 6 unchanged
D precedence = P0 > P1 > P2 > P3 > P4 > P5 unchanged
D HIGH / CAUTION / NO_HIGH_RISK_SIGNAL mapping = unchanged

Coverage denominator members = 5 baseline + 2 conditional families unchanged
INPUT_INSUFFICIENT / SCOPE_MISMATCH family mapping = unchanged

E KR = unchanged
```

因此本次属于：

```text
SCOPE_ENTRY_METADATA_DELTA
not RULE_LOGIC_DELTA
not DISPOSITION_LOGIC_DELTA
not SOURCE_DELTA
```

---

## 3. C 57-fixture Reuse

### Reusable

旧 57 条对以下不变语义仍有效：

```text
15 rules positive/negative boundaries
missingness 7-state semantics
BF-C-04 context independence
SBP branch coexistence
multi-rule coexistence
release mismatch
family-level sepsis/dyspnoea scope behavior
```

因此：

```text
C historical fixtures reusable for unchanged rule semantics = YES
full 57-fixture rebuild = NOT_REQUIRED
```

### Not sufficient for 0.2.1 delta

旧：

```text
C-SCOPE-002
pregnancy/recent-pregnancy=TRUE
→ sepsis rule SCOPE_MISMATCH
```

它只证明 NG253 family-level scope，不证明：

```text
pregnancy / puerperium TRUE
→ current U03 whole-slice rejected before any C rule evaluation
```

因此需 targeted C scope-entry fixture。

---

## 4. D 48-fixture Reuse

### Reusable

旧 48 条仍验证：

```text
P0 integrity/release precedence
P1 HIGH precedence
P2 insufficiency fail-closed
P3 CAUTION
P4 coverage-complete NO_HIGH_RISK_SIGNAL
P5 conflict
family-level NOT_APPLICABLE semantics
version/currentness mismatch
```

因此：

```text
D historical fixtures reusable for unchanged D semantics = YES
full 48-fixture rebuild = NOT_REQUIRED
```

### Not sufficient for 0.2.1 delta

旧：

```text
D-P0-007 = outside overall population scope → P0
D-COV-007 = overall policy scope mismatch → no denominator
```

是 generic coverage，但没有显式证明 pregnancy/puerperium 已从 family-level exclusion 提升为 whole-policy scope exclusion。

因此需 targeted D/Coverage fixtures。

---

## 5. Required Targeted Delta

至少新增以下覆盖：

```text
TGT-CDE-01
pregnancy_or_puerperium = TRUE
→ overall U03 scope rejected before C rule evaluation
→ no baseline / conditional rule execution as current-slice governed evaluation

TGT-CDE-02
pregnancy_or_puerperium = TRUE
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_MISMATCH

TGT-CDE-03
pregnancy_or_puerperium = TRUE
→ coverage = NOT_APPLICABLE_AT_POLICY_LEVEL
→ baseline 5 denominator NOT_CONSTRUCTED
→ conditional families NOT_RESOLVED

TGT-CDE-04
pregnancy_or_puerperium = FALSE
+ all other overall-scope fields valid
→ overall scope entry permitted
→ existing C57 / D48 semantics remain applicable
```

这 4 条是 minimum positive/negative delta，但不足以完成 freeze，因为还有 scope-entry missingness ambiguity。

---

## 6. Scope-entry Missingness Freeze Blocker

0.2.1 coverage contract 当前要求：

```text
overall_policy_scope_satisfied requires
pregnancy_or_puerperium = FALSE
```

且已定义：

```text
TRUE
→ NOT_APPLICABLE_AT_POLICY_LEVEL
→ D09-P-001 / OVERALL_POLICY_SCOPE_MISMATCH
```

但以下状态尚未形成唯一 formal D09 handling：

```text
UNKNOWN
NOT_ASKED
NOT_ESTABLISHED
```

已确定的不变量只有：

```text
these states != FALSE
→ must NOT enter denominator
→ must NOT produce P3/P4
→ must NOT produce NO_HIGH_RISK_SIGNAL
```

尚未唯一确定：

```text
exact P0 reason_code / formal failure classification
```

因此建立：

```text
BLOCKER-FZ-CDE-021-01
= OVERALL_SCOPE_ENTRY_MISSINGNESS_FORMALIZATION_REQUIRED
```

该 blocker：

```text
!= reopen BF-CDE-01 content alignment
!= change 15 C rules
!= change 6 D branches
```

但必须在 0.2.1 candidate freeze 前关闭。

---

## 7. Reuse Decision

```text
C 57 fixtures = REUSABLE_FOR_UNCHANGED_RULE_SEMANTICS
D 48 fixtures = REUSABLE_FOR_UNCHANGED_POLICY_SEMANTICS
full C fixture rebuild = NO
full D fixture rebuild = NO

targeted scope fixtures = REQUIRED
scope-entry missingness formalization = REQUIRED_BEFORE_FREEZE
```

历史 fixture 的 review 不能直接改写成“已验证 0.2.1”；必须通过 delta review 明确记录：

```text
old fixtures reused by invariance argument
+
new targeted delta fixtures reviewed
```

---

## 8. Current Status

```text
BF-CDE-01 = CLOSED_FOR_CONTENT
Evaluation Reuse Assessment = COMPLETE
Bulk Fixture Reuse = APPROVED_IN_PRINCIPLE
Targeted Delta Eval = REQUIRED / NOT_BUILT
BLOCKER-FZ-CDE-021-01 = OPEN
new candidate identities = NOT_CREATED
new freeze = NOT_COMPLETE
C/D/E cross-consistency re-review = NOT_STARTED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
```

正确顺序：

```text
formalize UNKNOWN / NOT_ASKED / NOT_ESTABLISHED overall-scope handling
↓
build targeted v0.2.1 scope fixtures
↓
Medical + Technical/Eval targeted review
↓
only then create/freeze new affected candidate versions
↓
C/D/E cross-consistency re-review
↓
Gate B reconsideration
```
