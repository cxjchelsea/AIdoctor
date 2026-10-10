# U07 D5 隔离验证收口与集成就绪评估 v0.1

日期：2026-10-10。评估截止 main：86e8843197091c8c8172b7e4213537a31bdf0654。
受审实现：#389 HEAD 39b58d0f53c74bac0519a744e7fe26351c82aaa9，tree 50ac4a03186eda495357e43744d4c0eae1362356。
复审：#390 HEAD 43d19e3bba49d64f4b62762924f20839eefa2365。此评估为同一助手的证据收口，不是独立人员签署。

## 裁决

| 项目 | 决定 |
| --- | --- |
| 隔离 Source/Canonical Binding 实现验证 | VERIFIED_CLOSED_BOUNDED，限固定 synthetic profile / Spring/JPA/JDBC / MySQL |
| SB-B01 / SB-R01 | 保持 #390 的 CLOSED_BOUNDED；原 #388 对旧 HEAD 的失败结论仍有效 |
| 进入 clean main-based 集成候选设计 | READY，须限定文件清单并重新执行候选 CI |
| 当前堆叠分支直接合入 main | NOT_READY_FOR_BOUNDED_MERGE：范围超过本切片 |
| 精确 main-target 集成验证 / merge authorization | PENDING_NEW_CANDIDATE；本步不授权或执行合并 |
| 真实 D5 / F8 / P01 / APPLIED / dispatch | NOT_READY，未接入，owner CA 与全局 gate 不变 |

关闭的是该切片的实现验证工作及两项已复现问题，不是 U07/D5 整体完成或 AUTH-01..26 全通过。

## 证据准入与版本关联

| 证据 | 已核对范围 | 准入限制 |
| --- | --- | --- |
| #384 37aa52c3af0e83da2cd952af313caa1be2645d73 + #386 139fdce703e6339e082e33ebbc791b5aac9b0ae1 | 接入合同修订及隔离切片可实施范围 | 设计证据；不等于真实 owner 授权或 Stage C 测试 |
| #387 bb412f82371ac92696d1f26d1ff2e2d6dbafa5a6 | 初始隔离实现 | 其21例成功不能抵消 #388 发现的问题，不单独作为最终验收版本 |
| #388 d92f3a69882314913ede92e1787c33cf235452c2 | 两项错误行为复现及 REVISE_REQUIRED | 审查历史，不纳入修复验收源码；不得带入错误行为成功断言 |
| #389 + #390 | 修复后的最终实现、25例正确行为 oracle、有限关闭复审 | 本切片接受版本，不扩展到真实 D5 |
| #383 0d5969df939bf7d079fe3d4ddb35143204781951 | 隔离桥接8例；模块434例、1 skip、无失败/错误 | 独立历史背景；非本候选全模块结果，不将其最小生产修改顺带纳入 |

Source/Binding run：https://github.com/cxjchelsea/AIdoctor/actions/runs/38019433064，job 114116901331 SUCCESS。实际日志25 tests、0 failures/errors/skips、BUILD SUCCESS。合成 checkout 9d5a83cf1f04a5f5a9ae64a0ae0be4bf1fe12c3e 的 tree 与 #389 HEAD 一致。#390 只增加一份文档，不声称新 runtime run。

桥接 run：https://github.com/cxjchelsea/AIdoctor/actions/runs/38016524441，job 114107911655，重新读取原日志确认8例及434例/1 skip。该结果属于 #383 的版本，不与25例相加宣称一个最终集成suite。

本步未重新运行 Maven/MySQL；复核已完成的原始日志、精确 SHA、代码依赖与 git diff。reference_vectors.py --check 及本切片 diff check 已通过。远程 PR #383/#384/#386/#387/#388/#389/#390 均 open/draft、未合并，版本与上述匹配。

## 依赖与范围评估

当前链：#383 → #384 → #386 → #387 → #389 → #390；#388 是从 #387 分出的审查分支，不是集成依赖。
#390 相对 main 的 merge-base 是上述 main，差异54文件、5044 additions / 1 deletion。该集合包含D1–D5的workflow、Java、V7 MySQL/Oracle迁移及 ConsultationWaitTransitionService 去final修改。没有合并冲突不等于范围可接受，不应由本切片收口隐式批准这些祖先改动。

Source/Binding pom只编译工具类及真实 CanonicalBusinessEvent*.java；workflow只加载原V2及 disposable fixture。与main逐项比较 Foundation目录、V2 migration无差异。该切片没有依赖 D5-P application repository、桥接工具、去final修改或 V7；生产pom/Java未引用本工具包。静态依赖支持单独抽取，尚未形成或测试新的 main-target 候选。

## 建议的 clean main-based 文件清单

只从最终修复/复审内容抽取以下12个新增文件；不得整体 cherry-pick 堆叠祖先。

| 组 | 精确路径 |
| --- | --- |
| workflow | .github/workflows/u07-d5-source-binding.yml |
| module | tools/u07_d5_source_binding/pom.xml |
| Java | tools/u07_d5_source_binding/src/main/java/com/aidoctor/verification/sourcebinding/BindingCodec.java |
| Java | tools/u07_d5_source_binding/src/main/java/com/aidoctor/verification/sourcebinding/SourceBindingAdapter.java |
| Java | tools/u07_d5_source_binding/src/main/java/com/aidoctor/verification/sourcebinding/TestAuthority.java |
| test | tools/u07_d5_source_binding/src/test/java/com/aidoctor/verification/sourcebinding/SourceBindingTest.java |
| fixture | tools/u07_d5_source_binding/src/test/resources/golden.properties |
| fixture | tools/u07_d5_source_binding/src/test/resources/reference_vectors.py |
| fixture | tools/u07_d5_source_binding/src/test/resources/schema-mysql.sql |
| record | docs/current/06_开发单元/U07_D5_Isolated_Source_Canonical_Binding_Implementation_v0.1.md |
| record | docs/current/06_开发单元/U07_D5_Source_Binding_Targeted_Remediation_v0.1.md |
| record | docs/current/06_开发单元/U07_D5_Source_Binding_Targeted_ReReview_Bounded_Closure_v0.1.md |

相对 #386 为12新增文件、1017行；抽取后须重新核算相对最新 main 的范围。设计合同 #384/#386 作为固定 SHA 引用，暂不顺带复制全部设计链。本评估记录也可随候选增加，若增加须明确将清单改为13文件，不能继续声称12文件。

## 后续集成门禁

1. 在最新 main 创建隔离候选；核对上表文件完整字节等价、零生产源码/Shared Contract/Flyway改动，所有历史记录引用固定受审版本。
2. 对候选重新执行 Python golden 和实际25例 Spring/MySQL CI；记录候选 HEAD、main base、合成 checkout tree、run/job/log。既有 #389 成功不能直接转写新候选成功。
3. 对完整 main-target diff 做定向复审，确认无 #388 错误 oracle、无祖先依赖泄漏、test-only profile和DB权限边界保持。
4. 以上满足后才能进入独立的精确 main-target merge authorization 决策；本步不要求用户提前授权未形成的候选，也不执行任何合并。

此步骤是集成就绪的实际未完成工作，不是要求继续修复已关闭的 SB-B01/SB-R01。

## 保留事项

Oracle runtime、真实commit网络unknown/进程crash、生产KMS与授权身份、U06实际eligibility签发、F3/P01/U15/P02/U02共同资源/许可、Stage C durable receipt与幂等完成、F8线性化及APPLIED/outbox/dispatch均未在本切片证明。AUTH-01..26中与issuer/binding重叠的隔离机制证据可供后续引用，不标整项owner oracle通过。历史read与新effect权限仍分离。

下一步：Clean Main-Based Source/Canonical Binding Integration Candidate + Exact-Diff Review Preparation。完成候选验证后再决定合并；真实 D5 的后续 adapter 切片另行设计与验证。
