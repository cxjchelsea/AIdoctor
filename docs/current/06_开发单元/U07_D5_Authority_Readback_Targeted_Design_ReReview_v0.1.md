# U07 D5 Authority/Readback 针对性设计复审 v0.1

日期：2026-10-10。
受审：#384 HEAD `37aa52c3af0e83da2cd952af313caa1be2645d73`，base #383 `0d5969df939bf7d079fe3d4ddb35143204781951`。
原审查：#385 `3529b0dbdc2511f52de8f8d5b375f753cb701eaa`；原受审HEAD `2bd1a055155377d37fbd237bb99984678ffc1205`。
本次为同一助手定向复核，不是独立人员签署。合同全文及新增§8–11已读；核对原IdempotencyPort与StateCommitter相关路径，未运行新测试。

## 决定

TARGETED_DESIGN_RE_REVIEW = ACCEPTED_FOR_ISOLATED_SOURCE_BINDING_SLICE
B01 / R01 / R02 = CLOSED_AT_DESIGN_LEVEL_ON_REVIEWED_HEAD
NEW_BLOCKER / REQUIRED = 0 / 0
ISOLATED_SOURCE_BINDING_DESIGN_READINESS = READY_WITH_STATED_BOUNDARIES
P01_STAGE_C_IMPLEMENTATION_VERIFICATION = NOT_PROVEN
REAL_D5 = NOT_READY
MERGE = NOT_AUTHORIZED

本决定只接受新接入合同对三项问题的设计修订。不能据此声称事务故障已经修复、AUTH测试已经通过，或关闭实际owner CAs。#385记录原HEAD的问题仍有效；本文件记录其在新HEAD的设计处置。

## 证据范围

| 核对项 | 证据与结果 |
| --- | --- |
| 当前PR | #384 OPEN/draft；HEAD与上述SHA一致，仍仅一个合同文件 |
| 修订差异 | 原HEAD到新HEAD一个commit、一个文件，130行新增/7行删除 |
| B01实物接口 | IdempotencyPort允许RESERVED及COMPLETED；complete返回void，故adapter必须另行核对receipt/projection；core捕获complete RuntimeException不能确认outer commit |
| B01原路径 | StateCommitter先reserve，repository结果成功后assembler.committed、complete、emit、return；新增合同没有把这个return当外层提交 |
| v0.2状态表示 | 文件名保留v0.1以保持引用，标题/正文明确v0.2及复审状态；无Java DTO/Shared Contracts注册 |
| 测试 | AUTH-01..26均为未来oracle；#383既有8例桥接证据不扩大为P01/issuer/F8实现证明 |

## 三项处置

| 原finding | 修订位置与成立理由 | 结论 |
| --- | --- | --- |
| B01 外层commit/幂等未绑定 | §3.5与§8明确mutation、不可变receipt、reserve/complete同guard-DB物理事务；内部COMMITTED暂存，只有最外层成功才发布。rollback-only不可清除；仅事务仍有效且完成投影尚未写入的可恢复失败可提交receipt+RESERVED。新事务先exact receipt再幂等核对，修复只有同effect projection、不再mutation；UNKNOWN+ABSENT不武断失败。AUTH-15..19覆盖原要求 | DESIGN_CLOSED |
| R01 签发/消费复验欠具体 | §9选定受控test-only composition root/issuer、提交record、配置route及每consumer复验；公有DTO、status或digest不充当身份。test issuer不得注册真实profile；当前epoch与撤销在同guard行序列化，不能远端bool替代。AUTH-20..23明确伪造/ref混淆/撤销竞态 | DESIGN_CLOSED |
| R02 结果组合不封闭 | §3.3正式列DEPENDENCY_BLOCKED；§10明确read/prepare状态、write attempt/durability/operational组合、claim依verdict约束及UNKNOWN无confirmed字段。历史read结果与当前effect权限分离；PROVEN_MISMATCH由F8原precedence评估。AUTH-24..26补非法组合及权限历史负例 | DESIGN_CLOSED |

B01兼容现有core的必要前提是新adapter包住完整治理边界，包括core replay路径：不得未经receipt关联核对就把COMPLETED_SAME_FINGERPRINT.originalResult当Stage-C完成。§8.2的COMPLETED+receipt匹配及§8.3新事务恢复已规定该约束；实现审查必须检查早返回路径，不能只查新mutation分支。

AUTH-10中的projection repair按§8.3仅限receipt/幂等派生证据，不得对丢失的真实Question/Gap/Pending内容补第二patch。真实内容与receipt不一致应隔离而非修临床状态。这一解释来自修订的明确单patch及禁止第二临床mutation规则，不扩大自动修复权限。

## 原规则回归核对

- 21字段typed binding/alias与payload-integrity规则保持，未新加指纹字段。
- F8 Tier0历史决定不变；Tier1沿原优先级，deadline相等EXPIRED，statement-time及Consultation/U15共同guard不变。
- checkpoint缺失不改业务verdict，原eligibility issuance仍不可由U07编造。
- Stage C保持一个Question/Gap/Pending patch；expectedCurrentValue不绕过现有core；actual content adapter仍须证明CAS。
- Stage C receipt只确认Stage C；P02、Stage D、Stage E同commit APPLIED/outbox、Stage F独立grant仍是前提。
- 不重写已占用V7，不把canonical+application桥接当canonical+binding实现。
- 真实source/permission/store/U15/F3/P01/P02/U02 owner CAs全部保持原状态。

## 下一隔离切片的可实施范围

下一步：U07 D5 — Isolated Source/Canonical Binding Adapter Implementation。

| 允许纳入 | 本切片验证要求 |
| --- | --- |
| 固定PROFILE-B/test-only issuer与nonclinical record store、受控manifest | trusted配置；伪造DTO/错issuer/profile/tenant/type/ref拒绝；生产profile不能加载test issuer |
| Source resolution与record reference verifier | source读取失败与拒绝分开；source字节digest及原issuance关联，不造真实许可 |
| canonical+21字段binding原子存储、exact reader | 同一事务；同key alias不覆盖原绑定；payload替换冲突；单表/内容不一致不handoff |
| typed结果及字段校验 | 先实现本切片实际使用的source/binding结果；非法status/字段组合fail-closed，不伪造F8/effect结果 |
| 隔离故障测试与真实编译执行 | 证明binding写失败canonical回滚、已提交记录读回、并发same-key/alias winner范围；明确MySQL与Oracle各自证据，不互相代替 |

本切片不顺带实现或授权真实F8 finalization、P01 Stage C、Clinical State写入、APPLIED、dispatch。B01合同可留待后续Stage-C adapter验证；不能为了本切片mock出“真实D5成功”。未绑定owner需保持DEPENDENCY_BLOCKED。

使用仅测试schema/fixture时不进入生产Flyway历史；若需要正式migration，必须先核对实际迁移链并选择下一未占用version，不能直接复用RDP示例V7。独立源码、schema和fixture范围须在实现PR中可审阅。

本轮交付为设计复审记录。没有新测试执行、源码/Shared Contracts/迁移修改、真实payload或merge；进入下一隔离实现不意味着真实D5就绪。
