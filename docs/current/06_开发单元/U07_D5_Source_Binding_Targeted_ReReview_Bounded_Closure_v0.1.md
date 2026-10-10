# U07 D5 Source/Canonical Binding 修复后定向复审与有限关闭裁决 v0.1

日期：2026-10-10。结论：PASS_WITH_BOUNDED_CLOSURE。
受审 PR #389 HEAD：39b58d0f53c74bac0519a744e7fe26351c82aaa9。
实现 base #387：bb412f82371ac92696d1f26d1ff2e2d6dbafa5a6；原审查 #388：d92f3a69882314913ede92e1787c33cf235452c2。
这是同一协作会话中的代码/证据定向复审，不声称另有独立人员审查。

## 范围及裁决

复审重新读取 adapter、全部25项测试、BindingCodec、实际 Foundation ledger、workflow 和修复差异；只裁决 SB-B01、SB-R01。#389 相对 #387 为3文件、108 additions / 4 deletions；Foundation、Shared Contracts、Flyway、生产入口无修改。

| Finding | 代码与测试证据 | 裁决 |
| --- | --- | --- |
| SB-B01 | target 明确要求 USER_ANSWER frame、无 target 引用且有答案密文；checkBinding 交叉核对 Foundation、binding 列/frame、原 issuer proof，重算 payload、scoped key、answer handle 并验证答案摘要。原腐化复现转为拒绝 oracle：INTEGRITY_CONFLICT、无 confirmed ID、无新增 pair；合法 resume 用例保留并通过。 | CLOSED_BOUNDED：隔离 MySQL adapter 的目标类型/内容一致性缺陷关闭。 |
| SB-R01 | 初始 ABSENT 后 Foundation 返回既有 winner 时走完整 existing 校验；直接 JDBC 可见性查询不自动 flush JPA pending insert，可识别同 ID 已提交孤立记录。仅真正新 original 执行签发时间约束。晚到匹配 alias 返回原 canonical ID，保留原时间、零 alias row；晚到不同 Question 为冲突，同 ID orphan 保持 INCONSISTENT、不补 binding。 | CLOSED_BOUNDED：已复现的晚到 alias 误判及相关孤立记录边界关闭。 |

未发现本轮范围内新增 blocker/required finding。该裁决不是全局无缺陷证明或 merge authorization。

## 实现语义检查

成功结果仍仅在外层 TransactionTemplate 完成 commit 后公开。新增 visible-winner 查询不是写许可；existing 仍验证 owner key/type/consultation/digest、完整 frame、issuer 来源及答案内容。若 ledger 已建立 pending insert 后出现竞争，最终 flush 仍可能触发1062；外层完成回滚后再以 fresh transaction 读 winner，未引入同一失败事务重试。既有 pending unique-index race 测试通过。

晚到测试 wrapper 仅委托真实 repository 并在首次 ABSENT 查询后等待另一线程的真实事务提交；ledger、JPA/JDBC、MySQL没有被 mock。匹配用例刻意使用不同 incoming occurred_at；负例保持同 storage key 而改变 Question，证明 winner 校验没有降为仅比较 Foundation payload。root fixture 腐化用于读回完整性验证，不宣称普通 consumer 有修改历史记录的权限。

## 执行证据及限制

- 实际 workflow run：https://github.com/cxjchelsea/AIdoctor/actions/runs/38019433064。
- job 114116901331：SUCCESS；实际日志 Tests run: 25, Failures: 0, Errors: 0, Skipped: 0；BUILD SUCCESS。
- 实际 checkout 为合成 merge 9d5a83cf1f04a5f5a9ae64a0ae0be4bf1fe12c3e；其 tree 与受审 HEAD 均为50ac4a03186eda495357e43744d4c0eae1362356。
- 受审 PR 保持 open/draft、未合并，head/base 与上述一致。证据评论：https://github.com/cxjchelsea/AIdoctor/pull/389#issuecomment-6093157210。
- 复审未重新运行 Spring/MySQL suite；复核已完成的精确内容 CI 日志。文档分支不把原 run 冒充为新文档 HEAD 的测试。
- 本地 reference_vectors.py --check 和 git diff --check 复核通过。

此证据只证明隔离 synthetic issuer、Spring/JPA/JDBC/MySQL 范围；不覆盖 Oracle runtime、真实网络 commit unknown、进程 crash 或真实 owner 安全授权。现有故障注入与丢响应用例不升级为这些未执行场景的证明。

## 后续边界

允许进入隔离 Source/Canonical Binding 的验证收口与集成就绪评估：重新核对堆叠 PR 的依赖和精确差异，决定接受哪些隔离实现证据及是否具备后续 merge authorization 前提。此处不执行合并。

真实 owner / F8 / P01 / APPLIED / dispatch 未接入；实际 owner CAs、全局 audit gate 不变，真实 D5 仍 NOT_READY。两个 finding 的有限关闭不能替代上述合同或真实接入验证。
