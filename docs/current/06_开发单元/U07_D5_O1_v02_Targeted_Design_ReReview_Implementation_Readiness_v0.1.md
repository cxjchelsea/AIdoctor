# U07 D5-O1 v0.2 定向设计复审与实施就绪评估 v0.1

日期2026-10-12。受审#412 HEADcbc61c939e61ea07496db8dec5269fe764b12a1d；main750770e81af79b764cc4148d7fa856363c89972a，远程均未变化。仅设计复审，未修改设计/实施/执行数据库测试/合并。同一助手审查，非独立人员批准。

## 结论及旧问题状态

DESIGN_RE_REVIEW=REVISE_REQUIRED；D5_O1_IMPLEMENTATION_READINESS=NOT_READY。剩1个blocker、1个required澄清，不要求扩大切片范围。

| 旧finding | 状态 | 依据 |
| --- | --- | --- |
| O1-B01 状态/版本/输入 | PARTIALLY_REMEDIATED，未关闭 | INVALID_INPUT、effect COMMITTED/prior+1、thread WAIT_CHECKPOINTED归属、MATCH必要状态已修订；排序仍有硬冲突被STALE短路的问题 |
| O1-B02 scope/production | BOUNDED_CLOSED_DESIGN_ONLY | 完整immutable TEST_SCOPE绑定、manifest/profile/kind精确检查、固定URL、production/未知profile无条件拒绝，禁止user_id推actor/tenant |
| O1-R01 快照/可执行fixture | PARTIALLY_REMEDIATED，未关闭 | MySQL/InnoDB/RR/同物理连接、DDL顺序、13文件清单、barrier和保留约束方案已补齐；startup SQL与授权前零连接承诺冲突待澄清 |

原有六类生产owner阻断继续OPEN；REAL_D5=NOT_READY；现有129隔离基线不会因此失效，也不是本设计已执行证明。

## O1-B01-R2：STALE优先级可能掩盖当前链硬损坏（BLOCKER）

3.4矩阵规定“按序号执行，第一条匹配即返回”：
12请求run/checkpoint不是当前指针→STALE；
13当前checkpoint状态/版本、parent/question/delivery错误→INCONSISTENT；
14consultation版本小于effect committed版本→INCONSISTENT。

反例：当前checkpoint存在且同scope，但parent_wait错误；请求是同scope的旧checkpoint。12的“当前checkpoint存在且scope关联一致”成立，先返回STALE，13的parent硬损坏未检查。另一反例：旧请求同时consultation.rowVersion<effect.committedRowVersion，12吞掉VERSION_REGRESSION。最后段落只要求STALE前核验current对象scope，仍没有规定完整current链硬完整性优先。

10 THREAD_WAIT_NOT_ESTABLISHED也在14之前：合法thread ACTIVE空指针且consultation版本回退，会先返回STALE。仅交换12/13并不能解决所有短路。

必要修订：把查询/校验分为确定的阶段，按实际已存在的当前链可验证内容先校验硬错误，再做STALE归类。current effect的版本合法性及consultation低于effect版本，先于thread未建立/请求旧/高级版本等STALE退出；已建立thread时当前run/checkpoint完整性（scope、枚举、parent、question、delivery、captured version）先于请求是否current。不存在尚未建立的checkpoint不强求读取，允许正常阶段STALE，但任何已观察到的硬错误不得藏入STALE。根ACTIVE无wait可明确早退，仅是NO_CURRENT_WAIT，不宣称其余链完整。

给出互斥phase流程和对应矩阵，而非“按表优先”与段落例外双真源。组合验收至少：旧请求+错parent、旧请求+错question/delivery、旧请求+版本回退、thread未建立+版本回退、current链完整+旧请求正常STALE，以及优先级固定reason。不需要修改scope/生产业务语义。

## O1-R01-R2：startup preflight与请求前置零SQL冲突（REQUIRED）

3.2规定reader“任何SQL/连接创建前做input和scope检查”；3.3又要求“Reader startup preflight使用reader账号验证schema/ENGINE/shape”。3.4 matrix先input/scope后资源；5验收要求invalid/scope前置拒绝零connection/SQL。startup不带Request，不能先通过请求authorizer。实现若在构造时调用preflight会违反字面零SQL；若为通过拒绝测试延迟preflight，需明确合同允许的资源生命周期。

必要修订建议（无需增加能力）：资源工厂仅执行纯内存profile/URL/manifest校验和创建lazy DS，不开连接；reader read先input/scope/outer检查，再在获授权的请求内建立事务和执行metadata preflight/物理资源检查，随后第一owner SELECT建立snapshot。每次拒绝路径通过计数验证零connection/SQL。若保留单独startup preflight命令，必须单独显式调用并区分其资源I/O，不将该I/O排除后仍声称整生命周期零SQL；最简方案删除自动startup I/O。

六表engine/shape preflight必须确定何时完成及是否缓存；不能让缓存观察混同每次snapshot认证。metadata preflight不作为首个owner MVCC读；重复授权请求与preflight测试的计数边界必须明确。配置profile/URL硬拒绝在工厂创建连接前仍保留；DB故障产生UNAVAILABLE且无payload。

## 实施就绪门禁

13文件inventory、独立JDBC模块、SELECT-only六表、fixture root分离、固定DB URL、authority UNKNOWN、无F8输出方向均可保留。实施前仅修订以上两个合同点；不需要为设计修订新增生产写方认证/完整runtime平台或更多无关接口。

本轮依据是重新读取固定设计全文及其矩阵；没有新runtime/MVCC/DDL执行证据。下一步在#412形成v0.3定向修订，然后复审两个剩余点并评估隔离实施就绪。两项关闭前不实施，不合并。
