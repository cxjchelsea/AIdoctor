# U07 D5 隔离 Source / Canonical Binding 实现 v0.1

日期：2026-10-10。设计基线 #384 `37aa52c3af0e83da2cd952af313caa1be2645d73`；复审基线 #386 `139fdce703e6339e082e33ebbc791b5aac9b0ae1`。
范围：`tools/u07_d5_source_binding` 独立Maven工具模块与专属CI；不注册production bean、不改Shared Contracts、Foundation源码或Flyway历史。

## 实现边界

- 固定PROFILE-B synthetic环境和MySQL `127.0.0.1:33323/u07_source_binding`；只有test composition root可以创建issuer/adapter。所有适配类package-private，主应用没有模块依赖或入口。
- 两个数据库角色：issuer只可SELECT/INSERT权威记录、更新permission epoch；consumer只可读取authority/epoch，SELECT/INSERT canonical及binding，无UPDATE/DELETE权力。fixture root只在测试中注入腐败/DDL故障。
- TestAuthority签发synthetic SOURCE record；这是synthetic原始issuance fixture，不是实际U06 eligibility发行。consumer按ref读取committed record，复验issuer/type/policy/manifest/profile/scope/key/bytes/digest，拒绝客户端DTO认证。
- 固定manifest由代码测试配置绑定；不接受request替换。Request仅有eventId/token/sourceRef/occurredAt，Scope由可信测试入口注入。没有HTTP/auth生产实现。
- 21字段binding与Foundation payload digest分别编码；alias ID/timestamps不进入指纹。nullable checkpoint为typed NULL，版本canonical decimal；原event时间不可改，alias时间不覆盖它。
- binding/answer handles由scoped stable key派生。synthetic plaintext不做语义归一化；答案使用AES-GCM加密保存到issuer及binding行，key-ref在行内，nonce随机，固定16-byte fixture key仅用于工具测试，不构成实际KMS/生产加密方案。
- 正常写入：独占最外层TransactionTemplate，真实Foundation ledger/repository加入同JpaTransactionManager；验证JPA/JDBC物理连接。Foundation flush后写binding，成功结果在outer template提交完成前不发布。
- 唯一键1062/23000冲突：结束失败事务，然后新template事务读取完整winner+binding；不依赖Foundation catch路径继续查询。其他不明异常保守UNKNOWN，无confirmed ID，单次ABSENT不改变原UNKNOWN事实。
- 原已提交canonical缺binding为INCONSISTENT，禁止从retry伪造binding。完整frame、digest、cipher、原source record或target关系不符则阻断。
- permission epoch采用共享行锁：并发admission可以竞争唯一索引，issuer撤销需排他行锁；撤销先则拒绝新identity commit，写入先则提交历史保留，后续新写拒绝。这里只是admission权限，非真实Clinical effect共同fence。
- RESUME_REQUEST只引用同scope/wait的已提交USER_ANSWER，验证其exact binding和source proof；无新answer bytes，无F8/Runtime/P01/U02调用。

## 字节编码与独立oracle

本切片明确RDP的length framing实例：每项UTF-8以big-endian int32字节长度开头；typed NULL长度为-1，空值禁止；prefix也为同格式item，每字段由name item与value item组成。prefix分别为u07-event-binding-v1/u07-storage-key-v1/u07-payload-v1；hash为lowercase SHA-256。名字按21字段固定顺序，不添加字段。

`reference_vectors.py`是独立Python stdlib实现，生成并校验checked-in golden.properties；JUnit读取固定预期而不使用SUT结果生成期望。当前是隔离实现的明确编码选择，未修改Shared Contracts或全局Foundation摘要规则；后续跨语言/实际owner接入仍须等价复核。

## 验证与限制

当前本地：BindingCodec以`--release 8`真实编译通过；Python reference `--check`通过；diff whitespace检查通过。完整Maven/Spring/MySQL执行由专属CI硬门禁提供，提交时状态PENDING，实际run/job/HEAD证据将在PR评论记录。

测试覆盖创建/replay/alias/原时间、改Question/answer、新key同answer、跨scope/ID碰撞、非法profile/input、伪造ref/issuer/policy/type/manifest、frame/cipher篡改、依赖不可用、DB权限、两处提交前回滚、数据库binding失败、response丢失/UNKNOWN、legacy orphan、exact resume target、真实unique-index race及fresh winner read、撤销先/写入先序列、独立golden、非法结果组合。

不是Oracle runtime验证；没有Oracle adapter或正式schema migration，MySQL证据不得转写Oracle通过。未知commit采用保守结果，测试中提交前异常导致UNKNOWN并未复现实际网络commit丢失。response-lost case发生在确认commit以后。这两种窗口明确分开。

本切片不关闭Foundation全面reference audit、真实eligibility issuance或任何F3/P01/U15/P02/U02 owner CA；不证明真实F8 finalization、Stage C或APPLIED。本模块中的Result是隔离admission/read回DTO，不注册或冒充F8/P01 Shared Contract。真实D5仍NOT_READY。没有生产入口、真实患者payload、Clinical State mutation、dispatch或merge。

下一步是本实现PR的代码与CI证据定向审查；通过后再决定是否进入下一个隔离adapter切片，而不是将本切片当真实D5完成。
