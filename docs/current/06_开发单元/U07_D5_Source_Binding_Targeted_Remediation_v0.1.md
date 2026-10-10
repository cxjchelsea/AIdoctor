# U07 D5 Source/Canonical Binding 定向修复 v0.1

日期：2026-10-10。实现base #387 `bb412f82371ac92696d1f26d1ff2e2d6dbafa5a6`；审查 #388 `d92f3a69882314913ede92e1787c33cf235452c2`。
状态：TARGETED_IMPLEMENTATION_REMEDIATION，待真实CI及新HEAD复审。不是finding closure或merge决定。

## SB-B01：目标权威与类型一致性

checkBinding统一比较Foundation event ID/type/consultation/key/payload、binding列、21字段frame和原source proof。重新计算event payload、source token派生storage key及answer handle，不以binding自证owner类型。

target另外明确要求USER_ANSWER、target字段为空、有answer cipher；cipher/plaintext digest及原source内容按既有验证执行。合法issuer-issued RESUME record不能替代USER_ANSWER target，即使预先腐坏的owner/binding满足SQL FK/CHECK，也会INTEGRITY_CONFLICT并回滚新resume事务，禁止补造历史答案。

原审查target腐坏复现probe已改为正确行为oracle：拒绝、无confirmed target/canonical ID、无新canonical/binding pair。原正常target与非法target用例保留。

## SB-R01：晚到winner与alias

Foundation返回不同ID的winner，或调用之后通过不自动flush JPA的JDBC读取已可见canonical row时，转完整existing验证；合法alias返回原canonical，不再把ID不同作为INCONSISTENT。

该查询不是业务许可：完整owner/binding/source校验仍必需。若JPA已有pending insert且后续flush遇到unique race，仍完整结束原事务再fresh winner read。缺binding的历史/晚到canonical为INCONSISTENT，不从当前retry补造。时间校验只在实际新original路径执行，alias不同occurred_at不覆盖历史。

新增/转换回归：
- 晚到同binding alias，incoming时间与original不同：REATTACHED，同canonical，零alias row，original时间不变。
- 晚到同key但Question不同：INTEGRITY_CONFLICT，零alias row。
- 晚到同ID、已提交canonical但无binding：INCONSISTENT，binding保持缺失。

interleaving用delegating repository wrapper控制已读ABSENT后的winner提交；查询、Spring事务、Foundation与MySQL均真实，不mock ledger或数据库语义。

## 验证与范围

完整隔离suite为25个JUnit测试：原21例 + 两项转换后的正确行为回归 + 两项新增负例。独立Python golden与diff check本地通过；真实Spring/JPA/JDBC/MySQL suite执行结果由修复PR的精确HEAD/run/job评论记录，提交时PENDING。

受审错误行为断言和REVIEW387错误复现打印未带入修复验收。代码只修改工具模块adapter/test；未更改Foundation、Shared Contracts、Flyway、生产入口或原设计。MySQL证据不扩展到Oracle、实际网络commit unknown或进程crash。

真实owner/F8/P01/APPLIED/dispatch未接入，实际owner CAs和全局audit gate保持原状态；真实D5 NOT_READY。本修复交付须经代码/证据针对性复审才能决定两个finding的有限关闭；未合并任何PR。
