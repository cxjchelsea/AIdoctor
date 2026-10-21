# U07 D5-O1 定向设计审查 v0.1

日期2026-10-12；设计PR #412 HEAD693e2141703fadbcec5fbb00c473cfce8d5cbd1f；main750770e81af79b764cc4148d7fa856363c89972a。本轮只审查，不修改设计、不实施、不执行DB测试、不合并。同一助手审查，非独立人员批准。

## 决策

DESIGN_REVIEW=REVISE_REQUIRED；D5_O1_IMPLEMENTATION_READINESS=NOT_READY_PENDING_TARGETED_REMEDIATION。2个设计blocker、1个required item，均限新只读切片合同，不撤销#405/#408已合入的隔离证据。REAL_D5=NOT_READY；六类真实接入阻断继续OPEN。

设计方向可保留：读取现有六表、disposable合成数据、reader SELECT-only、structure与authority分离、没有业务receipt、禁止将snapshot缓存为eligibility。无需为了本切片先认证生产writer；但下面合同必须精确化。

## O1-B01 状态与版本分类未冻结

设计3.4未显式要求clinical_consultation.lifecycle_status=WAITING_USER便可判MATCH；只核对current指针和effect状态无法排除ACTIVE却残留指针的损坏数据。effect_status允许集合留待实现，rowVersion变化写STALE/INCONSISTENT未给唯一分支。文字“checkpoint WAIT_CHECKPOINTED”与源码不符：RuntimeThreadStateRecord有ACTIVE/WAIT_CHECKPOINTED/AWAITING_USER，RuntimeWaitCheckpointRecord创建checkpoint_status=ACTIVE。

新增结果enum也没有INVALID_INPUT，输入校验失败的typed返回尚未定义。设计只将根不存在映射ABSENT，未定义正常ACTIVE且无wait链与半完成wait链的区分。该缺口导致实现/测试各自选择语义，且可能将未准备好状态误判MATCH。

必须修订：
- 冻结MATCH的所有必要状态：consultation WAITING_USER、effect COMMITTED、expected_prior_lifecycle ACTIVE、committed_lifecycle WAITING_USER、thread AWAITING_USER、checkpoint ACTIVE；版本非负及effect prior/committed关系必须明确，不假定row_version等于clinical version。
- 给出完整分类优先级及矩阵：根不存在、ACTIVE无wait、WAIT_CHECKPOINTED过渡、缺指向对象、current指针已移动、rowVersion大于/小于effect版本、unknown enum、关联错scope、invalid input。
- thread中间态是STALE/NOT_READY结构诊断，不当checkpoint枚举；正常无wait不等同损坏关联。模型可保留STALE并给reason NO_CURRENT_WAIT，不必新增无用业务status。
- 不强行要求run OPEN/CLOSED与F8语义，保持authority UNKNOWN；但未知run/authority枚举属于非法fixture或unsupported manifest，明确失败类型。

依据：ConsultationRecord.ACTIVE/WAITING_USER；ConsultationWaitEffectRecord构造effect_status COMMITTED、prior ACTIVE/committed WAITING_USER；RuntimeThreadStateRecord.reserve与enterAwaitingUser；RuntimeWaitCheckpointRecord.status ACTIVE。

## O1-B02 Scope证据校验与生产禁用不完整

ScopeAuthorizationPort返回ALLOW及“绑定这些ID的attestation”，但未定义结构、binding digest/版本、缺字段/错绑定拒绝、reader自检责任。仅信任布尔ALLOW不足以验证受限的fixture边界。production profile条款只说“无可用authorizer时UNAVAILABLE”，留下配置一个fixture authorizer后误启production的空间；该切片设计明示仅synthetic，应硬性拒绝全部非批准profile与未知DB地址，无论authorizer返回什么。

必须修订：
- 定义immutable TEST_SCOPE attestation，至少绑定request四ID、test actor/tenant、profile、fixture manifest/schema引用及scope证据版本；reader检查精确绑定及test origin，空/未知/不匹配fail closed。不把其称真实签名权威，不为本slice引入不必要密码协议。
- authorizer必须在任何SQL前执行；拒绝/未知/异常返回零row payload和typed原因。输入invalid先失败；DB根不存在的授权测试使用已批准固定scope ID集合，不靠先读DB再生成ALLOW。
- production/unknown profile在资源创建或入口处无条件禁用；任意authorizer ALLOW不得绕过。fixture凭证和URL固定disposable allowlist，不允许任意部署URL。
- user_id不得推导tenant/actor；TEST_SCOPE仅合成映射，保留真实O-B01未闭合。输出字段也必须声明属于授权后的最小结构数据，DENIED/UNAVAILABLE不泄漏查询命中情况。

## O1-R01 快照与fixture可执行前提不足

设计声称REPEATABLE_READ单connection snapshot，但现有V2/V3/V6 DDL没有在每表显式ENGINE=InnoDB；默认engine/会话隔离/autocommit不是可静态推定事实。同connection并不自动证明所有SELECT均为一致非锁快照，查询开始时间/读方式/异常映射需明确。DDL加载顺序、模块文件清单、DDL与约束完整性也尚未冻结；“重复关联/无约束模拟损坏”可能为测试擅改现有owner PK/唯一约束。

必须修订：
- 固定验证MySQL8、六表InnoDB、有效REPEATABLE_READ、autoCommit=false、同physical connection、普通非锁consistent SELECT；运行时前提失败UNAVAILABLE且不输出MATCH。snapshot从事务第一个一致性读建立，不能把BEGIN时间当快照时间。
- 固定V2→V3→V6执行到新schema，记录精确source SHA/DDL blob或digest；不改旧迁移。新增模块具体Java/test/bootstrap/workflow/POM inventory与最小依赖须列明。
- barrier安排在首次consistent SELECT后，独立writer提交修改，再reader继续：观察同一旧快照；另开fresh read观察新状态。不能仅通过connection ID或两次相同值声称MVCC通过。
- 保留实际PK/unique约束。可用无FK的现有表构造跨关联损坏；不可为制造duplicate拆owner约束。真实多行逻辑需用现有非唯一业务关联查询fixture或单元结果集合，不虚报真实PK可重复。
- 权限GRANT仅六表SELECT，基线前后内容比对不通过包含敏感字段的日志泄露。如何断言没有F8决策能力应采用reader无对应授权/接口或静态依赖与权限测试，不把不存在f8表的查询失败当“零F8写入”计数。

## 审查范围及验证状态

重新核验main/设计HEAD未变，#412仅一设计文档。读取其全文及当前实际ConsultationWaitEffectRecord、RuntimeThreadStateRecord、V2/V3定义，与前轮已读checkpoint/coordinator字段核对。没有查询生产数据、重新跑129项或建立新的Spring运行证据。历史基线129项仍有效于其固定受测合并SHA，不当本设计测试结果。

## 下一步修订清单

在#412上形成v0.2：完成状态/版本/输入分类矩阵，冻结TEST_SCOPE结构与硬禁profile，冻结MVCC/DDL/权限/文件清单及可执行barrier测试。保留六类真实owner阻断和authority UNKNOWN。然后做修订后定向设计复审，未通过前不实施D5-O1，不合并设计或其他PR。
