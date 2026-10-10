# U07 D5 F8 SQL/Clock — Targeted Remediation + Test Re-Review v0.1

对象：PR #397修订HEAD `0ccfe62dc6d53aec929f06bad1cba2e5f6fba927`。审查输入#398 `25f7c372f3ac77ebe29e26f37160a085b3cd2a4e`；旧实现 `c82306fed1e2a1a6e2dadde0d84997a2fae65df1`。

**PASS_WITH_STATED_PROTOTYPE_BOUNDARIES。DF8-SQL-B01 / DF8-SQL-R01 = BOUNDED_IMPLEMENTATION_VERIFIED_CLOSED。** 仅关闭这两项原型实现问题，不关闭完整F8/真实owner物理等价或D5。修复与复审由同一助手完成，不称独立审查。

## 精确修订与复审

相对旧实现仅2文件，39行新增/2行删除：finalize.sql与SqlClockTest.java；无schema/权限/workflow/Source/生产修改。原25例保留。

B01：pending exclusion新增current_wait IS NOT NULL，使NULL wait的P4不再被NOT UNKNOWN过滤。CASE优先级不改，P1/P2/P3仍在P4前；匹配pending仍阻断P5；缺authority仍由公共条件阻断，不将NULL等同充分证明。

R01：claim先按实际唯一wait_id识别；scope另在公共WHERE校验。wrong-scope行不再JOIN消失，原结构校验可见相关行。schema在本prototype规定wait_id全局唯一；另一wait_id/另一scope行是真正无关claim，不阻断当前wait。不宣称等同生产完整scope/question/parent-wait复合身份。

新增独立业务期望共9例：

| 组 | 数量 | 验证结果 |
| --- | ---: | --- |
| pending + cleared / moved / matching wait | 3 | REJECTED / REJECTED / 零行；原winner保持 |
| equivalent APPLIED + cleared wait | 1 | DUPLICATE，零own claim |
| wrong-scope malformed claim + legal / expired / cancelled | 3 | wrapper确定回滚，零decision；原scope/generation保持，不repair |
| 同scope半填claim + negative路径 | 1 | wrapper确定回滚，零decision；损坏行保持 |
| 无关wait/scope合法空claim + P7 | 1 | 当前ACCEPTED+claim完整提交；无关行保持 |

wrong-scope legal测试不创建新sentinel，直接执行conditional INSERT并确认0行导致回滚，证明不再依赖后续claim CAS兜底；expired/cancelled测试覆盖旧negative漏检路径。合法空sentinel、完整其他event winner、authority缺失、clock、故障、两种共同guard次序由保留25例覆盖。

旧反例来自源码审查，本轮没有另跑旧SQL故意复现；修订版本已真实执行新增期望，不冒称旧版red-run。

## 实际执行证据

精确HEAD `0ccfe62d...`，Java8 + MySQL8.0.46：

- [F8 run38030165467](https://github.com/cxjchelsea/AIdoctor/actions/runs/38030165467)，job114149234025：**34 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS**。
- CLOCK_ENV session/global timezone=SYSTEM，UTC_TIMESTAMP(6)样本 `2026-10-10T06:14:12.664380`；原型真实clock与固定边界仍分别覆盖。
- U03 run38030165473 / job114149233991：434 / 0 / 0 / 1 skipped。
- U02 run38030165483：Java job114149233673，434 / 0 / 0 / 1 skipped；Python job114149233894，11 + 9 + 1 passed。
- 本地git diff --check通过；无本地Maven/MySQL执行声明。Source25例未重跑，源码完全未变。

复审逐项核对SQL diff、结构gate和新增assertion，无新的定向blocker/required finding。34例并非全组合证明；prototype的聚合authority、测试辅助resource/attempt结果和手工跨期故障上限不变。

## 范围收口与下一步

这两项原型问题可有限关闭。下一步是**隔离F8 adapter实现就绪评估/实施清单**：确定Source/canonical同事务verifier、完整owner事实映射、Tier0/verdict-specific历史读回、typed result validator/异常与UNKNOWN边界的接入范围，再决定实施。不能仅凭本复审自动启动完整生产F8/P01或将34例作为真实owner物理等价证书。

真实D5保持NOT_READY；真实U06/U15/APPLIED/Foundation/P01相关CA不变。#397与本复审均为草稿，无合并。
