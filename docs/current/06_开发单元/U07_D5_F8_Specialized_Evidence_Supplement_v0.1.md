# U07 D5 F8 专项证据补齐 v0.1

基线：main702e280f14adacb50d6b72a6cd42cc7d32ff020a（#405标准合并）。本轮只添加测试及本记录，不改Java实现、SQL、DDL、权限或workflow，不合并。

## 执行设计

EV-F8-02两例：after_decision在同一consumer事务将新sentinel generation改为2，核对原CAS谓词匹配0，实际adapter CAS无法完成并回滚decision/sentinel，后续干净重试成功；after_sentinel通过真实MySQL唯一约束执行重复claim INSERT，捕获实际SQLException1062/23000并原样传播，验证ATTEMPTED/NOT_COMMITTED、零decision/claim、fresh ABSENT和重试成功。第二例为probe内真实F8表约束异常，不声称竞争writer使adapter自身INSERT发生1062，亦不构成生产并发认证。

EV-F8-03五例：先完整admit RESUME，再破坏独立target的canonical存在性、binding存在性、fingerprint、cipher及scope；核验finalize和fresh read的typed结果、NOT_ATTEMPTED、无receipt及零F8写入。仅missing-canonical fixture在root同一个Connection局部关闭FK检查以模拟既存损坏，并finally恢复；不更改consumer权限或实际schema约束。

原62 adapter、34 SQLClock、25 Source不得删改。新增7次测试，预期adapter69、F8总103、加Source共128；数量与关闭状态以真实CI日志为准。CI尚未执行，EV-F8-02/03保持OPEN/PENDING直到通过并复审；EV-F8-01 outbox仍OPEN/NOT_EXECUTED，FULL_CHECKLIST未完全关闭，REAL_D5=NOT_READY。

同一助手补证及定向核验，不称独立人员审批。本轮本地executor不可用，通过固定SHA读取仓库并发布隔离测试PR；编译与Spring/MySQL执行由Actions提供真实证据。
