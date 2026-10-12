# U07 D5-O1 PR #419 精确 main-target 差异复审与合并授权评估 v0.1

2026-10-12。EXACT_MAIN_TARGET_REVIEW=PASS_WITH_BOUNDED_SCOPE；MERGE_AUTHORIZATION_ASSESSMENT=ELIGIBLE_FOR_EXPLICIT_AUTHORIZATION。本轮仅评估，不授予用户执行授权，不合并。真实业务 REAL_D5=NOT_READY。

## 精确绑定

| 对象 | SHA / 状态 |
| --- | --- |
| 最新 main | 750770e81af79b764cc4148d7fa856363c89972a |
| #419 单提交 HEAD | d03180b8ee84e8407029ed73085c85fe3b3be7c7 |
| 候选 tree | 2b514fe83390d2c271c971ad545a13b522058699 |
| 已复审 #416 HEAD | ca9f0530681b4678ce74fc5802260bd044207c57；完整树与候选相等 |
| CI 实际 checkout | 76be8668c8a8dcde43a202f1f476184e91c4b708 |
| CI parents | 精确上述 main / #419 HEAD，顺序相同 |
| GitHub状态 | draft/open/unmerged；mergeable=true、mergeable_state=clean |

临时 PR test merge tree与候选相等。mergeable/clean不等于分支保护或独立人员批准，draft状态需在获执行授权后处理。未认证未读取的管理员规则，不绕过平台规则。

## 差异复审

远程13文件差异列表与本地精确main diff交叉核对：单提交、13新增文件、868行增加、0删除。完整tree和从#416到候选的空diff证明重整没有内容漂移。工作区干净，diff whitespace通过。

允许范围：独立pom、5 main Java类、2 test类、3 test资源、1只读权限workflow、1实现证据文档。无历史审查分支携入。生产源码/配置/Bean/路由、原V2/V3/V6、root build、F8/Source工具均未修改。
资源profile/URL为固定合成环境；请求与TEST_SCOPE前置校验；每请求同bound连接RR普通SELECT和非缓存metadata前提；PRIMARY角色与普通UNIQUE分开比较；当前链硬关联先于后续STALE；authority恒UNKNOWN、非MATCH无payload、MATCH仅最小不可修改结构观察。SQL标识符为内部固定值、请求参数绑定，无生产权限输入或F8最终写入口。

新增workflow仅pull_request触发，contents:read、checkout不保留凭证；disposable MySQL8/Java8，原DDL bootstrap digest验证、分离reader/writer。测试root只用于合成fixture；不访问真实数据库/患者。SELECT-only实际DML/DDL拒绝及MVCC证据沿用同树复审并在候选重新运行。依赖与MySQL8.0为现有冻结验证配置，不将未pin镜像patch/runner环境宣称完全可重现实验。

未发现该冻结隔离main-target差异的新阻断项。范围严格限工具结构验证，不能凭此关闭全部设计fault组合或生产接入。

## 精确候选验证

本轮再次核验三run completed/success；四job日志已在候选验证轮逐份核验绑定76be8668，本轮读取run状态及精确tree/parents。无额外新测试执行，不把再次读取记录作新执行。

| 执行 | job | 结果 |
| --- | --- | --- |
| [O1 38197838367](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197838367) | 114644403654 | 73；0failure/error/skip；BUILD SUCCESS |
| [U02 38197838363](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197838363) Java | 114644403580 | 434；0failure/error，1skip；BUILD SUCCESS |
| 同U02 Python | 114644403786 | 11+9+1 passed |
| [U03 38197838360](https://github.com/cxjchelsea/AIdoctor/actions/runs/38197838360) | 114644403645 | 434；0failure/error，1skip；BUILD SUCCESS |

73含真实MySQL PK→同列UNIQUE拒绝及恢复、真实MVCC、autoCommit/connection标识报告代理负向。代理不是实际数据库发生漂移。434重复执行不相加为独立覆盖；CD08参数临床harness skip不算通过。F8/Source129仍为未变main的历史证据，本轮没有重跑，不能计为新候选结果。bootstrap/diff本地检查已做，本地Java/MySQL没有执行。

## 授权评估与执行约束

建议可明确授权：仅将 #419 在上述固定main/head下使用 standard merge commit 合入 main，随后进行实际合并后验证。不得合并 #416、#417、#418或本评估PR，不以squash/rebase替代；不顺带关闭其他PR。当前 user 请求是“评估”，不是执行授权，所以 MERGE_EXECUTION_AUTHORIZATION=NOT_GRANTED。

执行前再读取最新 main、#419 HEAD、13文件范围、CI与平台合并条件；任一SHA/内容改变，当前评估失效，重新审查受影响差异。获授权后如需要，处理draft就绪状态，遵守平台规则；不能以已生成test merge冒充正式merge。执行后确认真正merge parents为固定main/head、tree为候选tree。

合并后验收必须使用实际merge SHA。由于O1 workflow仅pull_request，不承诺main push自动触发；必要时建立不合并的隔离验证harness，精确checkout实际merge SHA，运行O1真实Spring/JDBC/MySQL与既有受影响回归，并核验四job日志。不能复用76be8668临时test merge结果作post-merge结果。无需改main workflow或扩展业务范围。

## 边界与结论

#417两项finding已由#416同树修复并有限关闭；#418允许隔离候选阶段。完整合同NOT_FULLY_CLOSED；生产scope/action、delivery/eligibility、question/pending/current CDP、APPLIED/Source、生产事务拓扑/全部writer协议、outbox/Oracle/network-crash认证继续OPEN。MERGEABLE_TOOLS_SLICE不代表REAL_D5生产READY。

本次为同一助手定向复审，不声称独立人员批准。main未变；没有执行合并。下一步需要用户明确授权“仅 #419 standard merge commit + 合并后验证”。
