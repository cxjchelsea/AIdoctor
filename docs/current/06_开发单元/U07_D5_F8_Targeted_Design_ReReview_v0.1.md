# U07 D5-F8 修订后定向设计复审 v0.1

日期：2026-10-10。受审 #394 HEAD a941815cfe325bacb32089c6a106a221b273969e；正文v0.2，文件名保留v0.1。
原审查 #395 HEAD 142e47534508b024b878a03d92788cde203a8497；原受审12936fbf27cd632194cd3f97aa08457c28ced547。
结论：ACCEPTED_FOR_ISOLATED_SQL_CLOCK_PROTOTYPE；DF8-B01 / DF8-R01 = CLOSED_AT_DESIGN_LEVEL_ON_REVIEWED_HEAD。
同一助手复审，非独立人员批准；全文与修订矩阵重新核对，无新增测试执行或实现证明。

## 两项裁决

| finding | 修订核对 | 裁决 |
| --- | --- | --- |
| DF8-B01 | §5/9将观察版本稳定、scope/许可、共同guard、可信证据与clock列为公共安全条件；只有P7要求current wait匹配且无winner。P1显式允许已APPLIED winner及current wait移动/terminal，P2/P3/P4/P6保留可信负向事实，不再被共同WHERE过滤。未知高优先级事实不当false跳过，非法provenance仍阻断。R17/R18明确独立反例oracle。 | DESIGN_CLOSED |
| DF8-R01 | §10首次F8 DML调用前标ATTEMPTED；零行及策略退出确认回滚后RETRYABLE_FAILURE/ATTEMPTED/NOT_COMMITTED，不明commit/rollback仍UNKNOWN。新sentinel仅随ACCEPTED提交，P7→P2时回滚包括sentinel且后续仍原identity；§11逐verdict关联检查，合法空sentinel与半填损坏分开。R19..22覆盖退出及合法/非法组合。 | DESIGN_CLOSED |

未发现本轮修订范围新增blocker/required。关闭只涉及设计矛盾；没有宣称最终SQL、schema、时钟、DB权限或共同fence已实现/验证。#395旧HEAD的REVISE_REQUIRED历史结论保留。

## 原合同回归与实施解释

- Tier0不可变decision优先，历史read action与新finalize/effect许可分离；不重判当前取消为历史REJECTED。
- Tier1 P1..P8原顺序保持；DUPLICATE需要独立applied证据/同内容同wait，不新增claim、不判断医学含义。
- 当前wait相等只作为P7业务条件；公共版本稳定是“已观察事实未变”，不能重新收紧为“必须处于合法WAITING_USER”。
- statement clock与判定同最终语句；相等EXPIRED；commit晚于deadline不重写合法历史ACCEPTED。
- decision后claim两语句仍须同物理事务持共同guard，CAS失败外层回滚，不提前发布。
- P7预判后自然跨期回滚是明示的保守退出：本次报告attempt失败，后续可原identity生成EXPIRED；不能提交本次暂存negative decision并留下new sentinel。
- 只读历史错误属于HistoricalDecisionRead；写attempt与fresh winner作为独立结果，不用历史winner掩盖本次失败。
- 已有合法空sentinel兼容不等于新adapter可持久空sentinel，不允许补造owner历史或重派winner。
- 22组future oracle不是22个已执行测试。固定clock equality oracle不冒充实际MySQL时钟认证。
- verifier抽取仍需既有Source25例回归；真实owner/U15共同fence CA及全局gate不变。

## 下一步允许的有限范围

可进入独立 SQL/Clock Prototype + Physical Equivalence Verification，先证明物理表达能力，不直接交付完整F8 adapter：

| prototype范围 | 必须证据 |
| --- | --- |
| disposable MySQL8 schema与最终conditional SQL | P1已有winner/current wait移动、P2/P3/P4/P6负向条件、P5/P8零行与P7唯一claim；引用固定synthetic认证记录，不采用caller布尔authority |
| clock mapping | 实际UTC_TIMESTAMP(6)候选长事务推进/同语句一致性/精度与时区、跨deadline；固定clock与真实clock证据分开 |
| common Consultation guard | synthetic owner写入与F8两种先后序列；不共guard阻断；明确非真实U15接入 |
| decision/claim/sentinel原子性 | decision后/claimCAS失败全部回滚；P7→P2回滚new sentinel；既有合法empty sentinel及corruption reader |
| typed结果 | DML零行、确认回滚、未知、历史读和fresh winner分离，不把unknown+ABSENT当失败 |
| 范围 | 独立工具/fixture/workflow、无正式Flyway或生产bean；禁止真实payload/Clinical/P02/U02/APPLIED/dispatch |

prototype应直接基于最新main或明确固定受审基线，设计文档引用精确SHA；不能隐式引入旧54文件堆叠链。实际代码清单和CI证据需独立可审阅。若该prototype不涉及Source verifier抽取，应保持Source源码不变；若涉及须明确差异与回归。

本结论是进入上述隔离验证准备的设计接受，不是完整F8实施/生产/merge授权。MySQL成功不证明Oracle。未来真实commit网络unknown/进程crash若未实测，继续标明未验证。

## 状态

TARGETED_DESIGN_RE_REVIEW = PASS_WITH_STATED_PROTOTYPE_BOUNDARIES
DF8-B01 / DF8-R01 = DESIGN_CLOSED
ISOLATED_SQL_CLOCK_PROTOTYPE_DESIGN_READINESS = READY
FULL_F8_ADAPTER_IMPLEMENTATION_READINESS = PENDING_PHYSICAL_PROTOTYPE_EVIDENCE
REAL_D5 = NOT_READY
MERGE = NOT_AUTHORIZED / NOT_PERFORMED

下一步：隔离SQL/Clock原型与物理等价验证；根据实测再决定完整隔离F8适配实施范围。
