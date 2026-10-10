# U07 D5 F8 SQL/Clock — Targeted Implementation / Evidence Review v0.1

审查对象：PR #397，HEAD `c82306fed1e2a1a6e2dadde0d84997a2fae65df1`，main/base `835441492d130de320bf3d49fe2b4be598a2b327`。六个新增文件，无生产/Source修改。审查输入：#394修订§9–12及#396原型边界。

**REVISE_REQUIRED：1 blocker + 1 required。** 本轮是同一助手的定向源码/证据审查，非独立人类审查；不修改实现、不执行新MySQL测试、不授权合并或关闭真实CA。

## DF8-SQL-B01 — P4 cleared-wait + pending winner被三值逻辑吞掉

位置：`tools/u07_d5_f8/src/test/resources/finalize.sql` 最后 `AND NOT (...)`。

可构造的可信fixture：owner.scope/versions/authority正确；current_wait=NULL；terminal=NULL；clock<deadline；同historical_wait有完整ACCEPTED claim/decision，但没有APPLIED证据。按#394矩阵，应P4 REJECTED，因为P4优先于P5。CASE及前一个OR块确实选中P4。

末尾pending exclusion计算：a IS NULL=true、w IS NOT NULL=true、clock<deadline=true、terminal IS NULL=true、current_wait=historical_wait=NULL。合取为NULL；NOT NULL仍NULL；WHERE不保留它，故最终INSERT零行，而非P4。不是CASE顺序问题，也不能把零行猜成正确业务拒绝。

这是源码与[MySQL官方NULL语义](https://dev.mysql.com/doc/refman/8.0/en/working-with-null.html)结合得出的确定反例；本轮尚未通过真实MySQL新probe运行。已有P4测试current_wait='moved'且无winner，未覆盖此组合。P3的terminal=CANCELLED让该合取false，掩盖清空wait的NULL问题。

修订要求：pending gate必须显式只排除“没有更高优先级事实且当前wait确认匹配”的P5，所有nullable表达式应具有明确二值业务含义。可以增加current_wait IS NOT NULL或使用严格定义的NULL-safe匹配，但不能使缺authority成为合法negative，也不能改变P1/P2/P3优先级。真实MySQL回归：pending+NULL currentwait+nonterminal+futuredeadline必须1行REJECTED；matching pending必须0行；moved pending必须REJECTED；P1等价APPLIED+cleared wait仍DUPLICATE。

## DF8-SQL-R01 — scope过滤将已存在损坏claim伪装成缺行

位置：同SQL `LEFT JOIN wait_claim c ON c.wait_id=o.historical_wait AND c.scope_id=o.scope_id`，以及下游 `c.wait_id IS NULL` absence判断。

schema的wait_claim主键仅wait_id。若同wait_id行存在但scope_id错误，该行被JOIN隐藏；c全部NULL。即使它半填、错误generation或指向损坏winner，也会被当作“无claim”。authority_complete=1没有检查原始claim，现有损坏sentinel测试仅同scope/generation2。

可推导fixture：owner未来deadline+合法wait+delivery，插入wait_id='wait',scope='other',winner=NULL,generation2。原始政策INSERT可产ACCEPTED；atomic wrapper随后CAS0行会回滚，所以**不能宣称完整wrapper已提交非法ACCEPTED**。但是把owner变EXPIRED或CANCELLED、调用无new sentinel的negative wrapper，SQL可以产EXPIRED/REJECTED并提交，绕过相关claim完整性隔离。即使拒绝业务执行，原型也未证明其所声称的相关claim fail-closed边界。

修订要求：先按实际唯一身份识别原始claim存在，再明确校验scope及结构；错scope/半填/非法generation不得借JOIN消失成absence。也可重设计完整scope/question/wait复合身份，但必须说明跨scope合法独立行与损坏关联的区别，不能只换主键掩盖关系检查。新增真实MySQL：错scope claim在P7及negative路径均零decision/零新sentinel，旧损坏行不repair；同scope合法空sentinel、完整winner及另一合法event claim仍按原合同处理。

## 已成立的证据与其上限

精确HEAD的run `38029683009` / job `114147810361`已完成success，现有25项0failure/error/skip。此前真实日志记录MySQL8.0.46、statement时钟推进和UTC样本、两类原子故障回滚。U03 run38029682999与U02 run38029683068已通过；Java各434项/1skip，Python11+9+1。新发现不否定这些已执行用例，也不使未覆盖反例自动通过。

可保留：固定equality vs实际clock明确分开、decision/claim成功原子性、现有fault/sentinel回滚、guard两种串行顺序、consumer owner/APPLIED权限拒绝。SQL全部P1–P8完整等价及related-claim损坏隔离**不能关闭**。

资源guard检测只是测试辅助断言；没有生产入口自动拒绝外层事务/不同resource。pending只读测试只检查表计数，结果矩阵未实现。跨期测试手动抛RollbackProbe，不等于完整wrapper真实跨期路径认证。这些均属于已声明原型限制，不额外冒充新生产blocker，但后续adapter必须补齐。

完整source/canonical认证、Question/Pending/U06真实authority、JPA共同资源、verdict-specific读回、typed异常/UNKNOWN/crash、Oracle与APPLIED真实owner等均未完成；不因25例绿灯关闭。

## 收口与下一步

DF8-SQL-B01 OPEN；DF8-SQL-R01 OPEN。允许下一步仅对#397做SQL谓词及claim存在/完整性定向修复，加入上述组合故障用例，在真实MySQL执行并保留原25例回归，再做新HEAD定向复审。原型政策/完整性收口暂缓；不跳到完整F8 adapter。真实D5仍NOT_READY，真实owner CA未变，无合并。
