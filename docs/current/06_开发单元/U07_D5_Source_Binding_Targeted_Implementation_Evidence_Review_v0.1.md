# U07 D5 Source/Canonical Binding 实现与证据定向审查 v0.1

日期：2026-10-10。
受审 #387 HEAD `bb412f82371ac92696d1f26d1ff2e2d6dbafa5a6`；base #386 `139fdce703e6339e082e33ebbc791b5aac9b0ae1`。
设计 #384 `37aa52c3af0e83da2cd952af313caa1be2645d73`。
本次为同一助手的定向复核，不是独立人员签署。没有修改受审实现，没有merge。

## 决定

TARGETED_IMPLEMENTATION_EVIDENCE_REVIEW = REVISE_REQUIRED
FINDINGS = 1 BLOCKER + 1 REQUIRED
ISOLATED_MYSQL_ACCEPTANCE_EVIDENCE = VALID_BUT_INCOMPLETE
SOURCE_BINDING_IMPLEMENTATION_CLOSURE = NOT_APPROVED
REAL_D5 = NOT_READY

原有21例确实通过，不等于全部实现分支正确。本审查另加两个仅用于复现错误行为的probe，真实数据库均复现；须修实现并将这两个probe改成正确行为回归oracle，不能将“断言错误行为且通过”称为验收通过。

## 证据与SHA归属

| 对象 | 核对结果 |
| --- | --- |
| #387状态/范围 | OPEN/draft/unmerged；10个新增文件、873行；Foundation/app/Shared Contracts/Flyway未改 |
| 原CI | run38018498229 / job114114051947，SUCCESS；21 tests、0 failure/error/skip，Maven BUILD SUCCESS |
| 原CI checkout | synthetic merge `a93a1d70715c8f5d082d8ed8ad52d55f53fbf6e3`，tree `698ff548cfedbfed787714c837017e6ac2bc7389`等于#387 HEAD tree；不是实际merge |
| review probe | #388 probe HEAD `ac94ff74137378091a4d3ae66459598f79fd3bcd`；相对#387仅SourceBindingTest新增45行，SUT原样 |
| probe CI | run38018805276 / job114115001685，SUCCESS；23 tests、0 failure/error/skip、BUILD SUCCESS；其中新增2例是已知错误行为复现断言 |
| probe日志 | REVIEW387_TARGET_TYPE_MISMATCH_ACCEPTED=true answer_bytes_absent=true；REVIEW387_LATE_ALIAS_FALSE_INCONSISTENT=true matching_winner_read=true |
| 文档提交 | 本记录在probe执行后添加；不声称新增文档HEAD已重新执行数据库suite，probe源与SUT不因文档改变 |

CI链接：
- https://github.com/cxjchelsea/AIdoctor/actions/runs/38018498229
- https://github.com/cxjchelsea/AIdoctor/actions/runs/38018805276

审查读取完整BindingCodec、TestAuthority、SourceBindingAdapter、SourceBindingTest、SQL fixture、POM、workflow及实现说明；核对原始job日志、当前PR SHA与差异。本地reference --check与diff whitespace check通过；本地无Maven/数据库执行，实际suite证据来自以上CI。

## SB-B01 — target Foundation/Binding event type交叉一致性缺失（BLOCKER）

位置：SourceBindingAdapter.target，受审行196–207；checkBinding行172–194。
target先确认Foundation owner_type=USER_ANSWER，但随后将binding frame解码为任意合法event type，再用该frame构造Source并调用checkBinding。后者只验证frame type与binding表event_type匹配，没有确认它们也与Foundation owner_type相同。target也没有独立要求frame必须USER_ANSWER、有答案字节、无target字段。

复现：reviewCorruptResumeBindingCanMasqueradeAsCanonicalUserAnswer。
1. 先真实提交一个合法USER_ANSWER pair。
2. 使用真实test issuer签发一个合法RESUME_REQUEST source record，保持相同scope/wait。
3. fixture root构造预先存在的owner/binding不一致：Foundation保留USER_ANSWER类型，但key/digest与binding改成该合法RESUME记录；binding类型为RESUME_REQUEST且答案字节为空，source record未被伪造或修改。SQL FK/conditional CHECK仍可满足。
4. 提交一个新的合法RESUME_REQUEST指向该目标。当前代码返回TARGET_REATTACHED并提交新resume pair，而非隔离；日志确认answer_bytes_absent=true。

这是隔离的legacy/corrupt-target完整性反例，不是普通客户端能突破UPDATE权限的生产攻击，也没有真实F8/P01/APPLIED。但合同已要求exact target必须原USER_ANSWER，owner/binding不一致不得成功，故阻塞本切片关闭。

最小修复：
- 统一验证Foundation owner字段、binding列、21-field frame及原source proof的一致性，target不得把frame类型自证为owner类型。
- 明确要求target为USER_ANSWER、frame target_answer_event_id为空，答案cipher/digest/ref有效；复算原source token派生key与payload digest及对应handle。
- 不一致返回INCONSISTENT或INTEGRITY_CONFLICT，不能补造原answer/binding；新resume事务零持久行。
- 将本probe改为预期拒绝、无新pair的oracle；补正常USER_ANSWER target仍通过，不能以全面禁用RESUME掩盖问题。

## SB-R01 — 初次查询之后出现的合法alias winner误报（REQUIRED）

位置：SourceBindingAdapter.write，受审行145–152，尤其event.getEventId()!=request.eventId即INCONSISTENT；Foundation真实ledger允许同key返回原canonical winner。

复现：reviewLateMatchingAliasWinnerIsMisclassified。
使用delegating repository wrapper只控制时序，所有查询/写入与Foundation ledger仍真实：
1. alias请求在write的首次findByIdempotencyKey读到ABSENT。
2. 返回该已读ABSENT之前，另一线程真实提交相同source/key/完整binding的原winner。
3. Foundation ledger后续查询正常返回该已提交winner，且没有unique异常。
4. 当前write因winner ID不是alias ID而返回INCONSISTENT；随后新事务exact read却返回REATTACHED及原winner，alias无持久行。

原pending INSERT race测试通过的是两方均进入INSERT、loser收到1062的路径；不能代表这种没有SQL异常的late-winner路径。此问题不产生假成功，但把合法幂等重附着当corruption，影响可恢复性。

最小修复：
- ledger返回既有winner时比较完整committed binding/owner/source证据，合法同key alias返回原canonical ID；不尝试第二binding INSERT。
- 也可完整结束当前事务后fresh winner read；必须明确成功/失败归属，不在污染事务内继续。
- original occurred_at只约束真正原事件，alias时间不得因“初次查无结果”被提前错误限制或覆盖历史。
- 修正probe预期REATTACHED、同winner、零alias row；保留不同payload/Question/wait及event-ID碰撞的CONFLICT负例。

## 本轮接受的部分及证据边界

| 关注点 | 结论 |
| --- | --- |
| 21字段、typed NULL与独立payload digest | 固定字段顺序及Python golden核对有效；未改全局Foundation摘要格式 |
| source trust | issuer/action/policy/manifest/profile/scope/ref读取复验及DB角色隔离已有实测；test issuer不代表U06实际authority |
| 正常atomicity | JPA/JDBC同连接；Foundation flush后/Binding insert后注入失败两表回滚；actual SQL故障无成功结果 |
| 既有unique INSERT race | 实际1062/23000、一winner、fresh transaction read有效；SB-R01是另一个分支 |
| epoch | admission共享锁与撤销排他锁阻塞实测；撤销先的顺序负例有效，未扩大为实际Clinical/U15共同fence |
| UNKNOWN | 不发布confirmed ID，ABSENT不改写原UNKNOWN；测试明确是precommit故障的保守分类，未证明真实网络commit未知 |
| crash | 本切片未执行进程强杀；不能借前D4其他实现的证据称当前模块crash验证通过 |
| 历史读与许可 | 当前Scope为可信synthetic测试注入；read不授新effect，不代表生产认证入口 |
| Oracle/真实owner | 明确未验证，不作为本轮额外finding，也不将其关闭 |
| 全局readiness | Foundation reference audit及实际owner CAs保持原前提；无真实D5完成声明 |

## 修订与下一步

下一步只修SB-B01和SB-R01，补正向/负向回归测试并在新HEAD真实执行MySQL suite，再做针对性复审。无需重开F8政策、添加生产入口或合并任何PR。#388当前probe保留错误行为断言仅用于审查证据；进入实现修复时应转换为正确行为oracle，不能直接混入已验收测试。

本轮只新增review probes与审查记录，#387实现保持不变；没有真实患者数据、Clinical写入、F8/APPLIED/dispatch或merge。审查结果REVISE_REQUIRED，不授予实现关闭或合并。
