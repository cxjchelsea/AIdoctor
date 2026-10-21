# U07 D5-O1 v0.3 定向设计复审与隔离实施就绪裁决 v0.1

日期2026-10-12。受审#412 HEAD1b58f4d4f3fd7ffc4536d698f1fc05852f9031ae；main750770e81af79b764cc4148d7fa856363c89972a。远程重新核验均未变，设计只一文档，未合并。本轮只复审、无实现/DB执行/合并。同一助手复审，非独立人员审批。

## 1. 裁决

DESIGN_RE_REVIEW=PASS_WITH_BOUNDED_SCOPE。
D5_O1_ISOLATED_IMPLEMENTATION_READINESS=READY。
READINESS_SCOPE=DISPOSABLE_SYNTHETIC_OWNER_SCHEMA_READBACK_ONLY。
IMPLEMENTATION_EXECUTED=false；IMPLEMENTATION_VERIFICATION=NOT_EXECUTED；MERGE_AUTHORIZATION=NOT_GRANTED；REAL_D5=NOT_READY。

可进入按冻结清单实施的下一步；本轮未自动执行实现。readiness不等于实际资源、MVCC、权限或业务owner CA已认证。全部六类真实接入阻断O-B01..06保持OPEN。

## 2. 旧finding关闭

| finding | 设计裁决 | 依据 |
| --- | --- | --- |
| O1-B01及O1-B01-R2 | BOUNDED_CLOSED_DESIGN_ONLY | P0..P10成为唯一流程；P6 effect硬错误/版本回退先于P7 thread未建立；P8当前run/checkpoint完整性先于P9旧请求/版本较新/WAIT_CHECKPOINTED；MATCH冻结根/thread/effect/checkpoint状态及版本关系 |
| O1-B02 | BOUNDED_CLOSED_DESIGN_ONLY，继承#414 | TEST_SCOPE完整字段及manifest/请求精确绑定，fixture port不得由请求注入，production/未知profile/非精确URL硬拒绝，无user_id推tenant/actor |
| O1-R01及O1-R01-R2 | BOUNDED_CLOSED_DESIGN_ONLY | lazy工厂零I/O；input/scope/outer检查后才创建事务/连接；每请求同bound connection非缓存preflight；InnoDB/RR/DDL/权限/barrier清单可执行，保留原PK/unique |

没有追加设计required finding。物理连接与快照机制的代码实现仍应在实现审查中检查，设计表述不算测试结果。

## 3. 关键反例复核

| 组合 | v0.3唯一出口 |
| --- | --- |
| 旧请求+当前parent/question/delivery损坏 | P8 INCONSISTENT/CHECKPOINT_LINK_INVALID，P9未到达 |
| 旧请求+根版本回退 | P6 INCONSISTENT/VERSION_REGRESSION |
| thread ACTIVE空指针+根版本回退 | P6 VERSION_REGRESSION，不能由P7 STALE吞掉 |
| 当前链缺对象+旧请求 | P8 INCONSISTENT/CURRENT_OWNER_MISSING |
| 当前链完整+同scope旧请求 | P9 STALE/REQUEST_NOT_CURRENT，无payload |
| ACTIVE无wait+未来checkpoint缺失 | P4 STALE/NO_CURRENT_WAIT，仅根状态观察，不声称子链完整 |
| current完整WAIT_CHECKPOINTED+旧请求 | P9固定顺序，REQUEST_NOT_CURRENT优先 |
| 工厂构造、invalid/scope/outer拒绝 | 零连接/SQL，不靠剔除startup统计满足 |
| 连续授权读取间schema前提损坏 | 第二请求重做preflight→UNAVAILABLE，无缓存MATCH |

以上为纸面合同推演，没有冒充新增数据库实测。P5请求run历史version不与当前checkpoint混比较；P8只比较当前run/checkpoint；P9旧checkpoint若存在须校验请求关联，旧非当前对象缺失不当当前指针损坏。既能诊断坏当前链，也保留正常部分阶段。

## 4. 受限实施合同

按#412 v0.3的13文件范围实施：
独立tools/u07_d5_owner_readback模块，5 main Java类、2 test类、3 test资源、pom、独立workflow、一份实施证据。初始实施分支从届时main创建；设计文档不靠批量合并历史PR带入。任何范围调整需写明原因与差异，不静默添加生产Bean/路由/迁移或通用框架。

只加载原V2→V3→V6到固定新disposable schema，记录源SHA/DDL blob/digest及schema manifest。root种合成数据；reader仅六表SELECT，writer测试账号独立。固定127.0.0.1:33325/u07_owner_readback与NONPRODUCTION_SYNTHETIC，不支持任意部署URL。选择版本及资源身份符合冻结设计。

构造及拒绝路径计数必须覆盖全生命周期；授权后事务内观察autoCommit/isolation/catalog/engine/shape，同physical connection普通consistent SELECT。barrier证明首次owner读后独立writer COMMIT而本reader仍观察旧完整snapshot，新reader观察新状态；metadata preflight不能替代MVCC测试或被称owner快照锚点。单schema测试不认证跨服务全局一致性。

结果authority恒UNKNOWN；只有MATCH含最小结构payload；没有F8 verdict/COMMITTED/APPLIED/eligible字段。全部typed exits及组合优先级验证，无误判MATCH。PK/unique保持；现有无FK关联可构造损坏，不拆约束制造重复PK实测。权限测试/关键摘要及依赖SQL清单证明reader无DML/F8能力；不靠不存在表查询失败冒充零写入。

## 5. 验证与后续门禁

当前129 Source/F8基线属于main750770e8合并后证据，未在本设计复审重新跑。实现阶段必须真实Java8编译、Spring/JDBC/MySQL测试；读回模块实测数量单列，资源故障代理和真实物理行为单列，不把unit oracle当DB认证。根据改动范围执行现有必要回归，不虚报未重跑基线。

完成后做实现与证据定向审查，再作有限验证收口/集成就绪和明确合并授权。任何真实scope/action、投递/eligibility、question/pending/current CDP、APPLIED/Source、writer共享锁或outbox接入须另行合同与认证。

下一步：D5-O1隔离现有Owner表读回实现与真实验证。此次仅判实施准备就绪，没有部署、真实患者数据访问或合并。
