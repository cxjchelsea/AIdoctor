# U07 D5 Source/F8/P01 权威读回合同针对性设计审查 v0.1

日期：2026-10-10。受审 #384 HEAD `2bd1a055155377d37fbd237bb99984678ffc1205`；base #383 `0d5969df939bf7d079fe3d4ddb35143204781951`。
范围：单文件 U07_D5_Source_F8_P01_Authority_Readback_Contract_v0.1.md，来源可信、F8 temporal/fence、P01 single patch/CAS/exact effect readback。
本次为同一助手的针对性复核，不是独立人员签署。无代码改动、新测试执行或合并。

## 决定

TARGETED_DESIGN_REVIEW = REVISE_REQUIRED
FINDINGS = 1 BLOCKER + 2 REQUIRED
ISOLATED_ADAPTER_IMPLEMENTATION_READINESS = NOT_READY_UNTIL_TARGETED_REMEDIATION
REAL_D5 = NOT_READY

不用重开原RDP设计，不增加新治理链。先补三项具体合同，再定向复审，之后进入隔离source/binding适配实现。

## B01 — P01 外层提交与幂等完成未绑定（BLOCKER）

位置：受审§3.5 commitExact及§4 P01读回；实物 StateCommitter.commit 的 attemptAtomicCommit → assembler.committed → idempotency.complete → emitSafely → return路径；IdempotencyPort；U06SyntheticP01Runtime内部map Idem。

现有core在repository返回COMMITTED后，立即生成CommitResult并完成幂等，然后返回。该结果原本只是机械repository边界，并不知道外层JpaTransactionManager是否已经提交。受审设计要求P01加入Stage-C共同guard事务，却没有定义以下情况：
1. owner state/effect先执行，core.complete写入非事务map或独立store，随后外层flush/commit失败；
2. core已经返回COMMITTED但实际commit结果未知；
3. complete失败被core捕获，reservation继续存在；数据库effect实际已提交。

因此可能留下“外层临床状态已回滚、幂等仍COMPLETED”的假历史结果，或已提交effect长期RESERVED而被盲重试。要求exact readEffect避免APPLIED是必要条件，但未定义commitExact发布点及reservation/receipt原子性，仍不足以指导适配实现。#383只验证Foundation/application桥接，不覆盖StateCommitter/IdempotencyPort。

最小修订：
- 明确owner mutation、durable effect receipt、idempotency reserve/complete的同guard-DB事务归属与状态转换；禁止map/独立commit成为Stage-C真实完成依据。
- core内部COMMITTED只能作为外层事务的暂存结果；真正COMMITTED的GovernedCommitOutcome必须由外层成功提交确认后发布，rollback/unknown不能传播暂存成功。
- 定义core swallowing complete failure的适配规则：允许保留reservation并查询已提交exact effect；不得因此重新mutation。若receipt存在但idempotency尚未complete，修复只能完成同effect的幂等投影，不能产生第二patch/version。
- 给出失败后新事务readEffect与idempotency reconciliation的先后；无记录不将未知提交强判失败；无需为本修订改机械core语义。
- 补验收：暂存COMMITTED后外层rollback/flush失败、commit unknown、complete失败+owner commit成功、成功commit后响应丢失；验证无假completed、无第二版本、原effect一致读回。

此为新接入合同的事务缺口，不宣称已在生产复现故障，也不凭静态分析否定#383的桥接PASS。

## R01 — 可信结果签发/消费复验机制未具体化（REQUIRED）

位置：§3.1 VERIFIED、§3.3快照、§3.4/3.5各种Ref、§4 manifest。

文档正确声明public DTO/owner名称不是authority，但没有选定SourceResolution/VerifiedAdmissionRef/OwnerConsumptionDecision等如何形成可信来源、以及下游如何排除调用方伪造。同样“manifest来自可信配置”没有限定test issuer与真实issuer的互斥适用边界。当前缺少真实issuer部署信息并不是本次设计blocker；可以先给出明确的隔离机制。

最小修订：
- 为下一隔离切片选定一个具体test-only签发与读取拓扑：谁可以创建已验证结果、持久化record如何按引用重新读取、consumer复验哪些issuer/profile/scope/version及fingerprint字段。
- request只可传untrusted ref；consumer不能仅见status=VERIFIED或同进程DTO便接受。选择受控内部调用/持久record复验或受审认证传输，不要求无依据引入签名系统。
- manifest中的owner/issuer身份由可信配置绑定，不接受caller替换；test issuer只在固定synthetic profile生效，真实owner未绑定保持不可用。
- 当前permission/revocation epoch在同effect临界区检查；说明它如何与owner mutation序列化，不能仅临界区前一次远端bool precheck。
- 补伪造VERIFIED DTO、换issuer/profile/tenant、替换ref指向其他effect、policy撤销竞态的负例。

已接受的21字段binding、alias与payload-integrity规则不重设计；本项不要求现在提供真实凭据或接生产issuer。

## R02 — operational outcome 与字段合法组合不封闭（REQUIRED）

位置：§3.3 WaitSnapshotResult列出COHERENT/PROVEN_MISMATCH/UNAVAILABLE/INCOHERENT/INVALID_PROVENANCE，却随后返回未列出的DEPENDENCY_BLOCKED；§3.4 DecisionCommitResult只列durability，另在文本描述DEFER。

实现者无法从合同确定“未尝试finalization”“数据库已尝试但未提交”“commit未知”“历史读回”哪些字段可存在；也无法确定PROVEN_MISMATCH怎样交给F8而不被admission提前升级为REJECTED。容易以默认值补decision/claim或把DEPENDENCY_BLOCKED当成功快照。

最小修订：
- 用一张封闭结果表明确各port允许的operational status/attempt/durability/read状态及证据字段可空性；DEPENDENCY_BLOCKED有正式位置。可采用正交字段，不强迫某一种enum命名。
- COHERENT是输入证据条件而非F8 ACCEPTED；可信PROVEN_MISMATCH由正式F8按precedence判业务结果，INVALID_PROVENANCE/UNAVAILABLE不得伪造业务verdict。
- DEFER/NOT_ATTEMPTED不能携带新committed decision/claim；UNKNOWN只能携带未确认的查询键/候选元数据，不能发布成功verdict；历史committed decision与当前effect permission分开。
- 补合法/非法字段组合验收，不把上述operational情况增为第五F8 business verdict。

## 通过的设计边界

| 关注点 | 复核结论 |
| --- | --- |
| 21字段binding与alias | 保留RDP-01 exact frame；distinct answer digest；RESUME_REQUEST不造新answer |
| F8 precedence/time | 正确保留历史decision、common U15 guard、最终conditional statement time及deadline相等EXPIRED；pending winner非DUPLICATE |
| F8/P02分离 | checkpoint missing不改business verdict；issuer/clock unavailable operational defer |
| P01语义范围 | 不继承U06固定权限；Stage C一个Question/Gap/Pending patch；更高当前版本先查exact historical effect |
| CAS兼容 | 明确现有core拒绝expectedCurrentValue；无direct CDP bypass；实际adapter条件写与common guard仍待设计/验证 |
| APPLIED/dispatch | Stage C不代表APPLIED；P02/D/E仍必须，F独立grant仍必须 |
| migration | 识别V7已占用，不重写原迁移或直接添加第二V7 |
| 证据范围 | #383仅基础桥接；14验收情景未执行；实际owner CAs未由文档关闭 |

本次未发现需要重开F8时间政策或改变RDP-03 APPLIED条件的问题。B01/R01/R02分别关乎新接入层的commit发布、可信消费、结果表示，而不是新增业务规则。

## 修订范围与下一步

只修改#384接入合同，新增明确的commit/idempotency协议、test issuer/ref-verification拓扑、结果组合表及相应oracle。保持Foundation/StateCommitter接口、RDP-01/02/03、migrations与实际owner CAs不变；发现必须改变它们时才单独记录具体amendment。

完成后针对这三项复审。设计接受之后，下一隔离source/binding切片先实现可信签发/复验和canonical-binding原子存储；B01的完整P01集成可留到Stage-C适配实现，但必须先有明确合同，不把设计接受等同于真实临床能力。

没有运行新测试；没有代码/Shared Contracts/迁移改动、真实payload、APPLIED、外部consumer或生产权限。#384保持draft/unmerged；本文件不授予合并。
