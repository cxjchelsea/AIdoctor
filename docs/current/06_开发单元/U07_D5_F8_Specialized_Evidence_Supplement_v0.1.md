# U07 D5 F8 专项证据补齐 v0.1

更新日期：2026-10-12。main基线702e280f14adacb50d6b72a6cd42cc7d32ff020a；PR #408隔离test-only分支，未合并。不改adapter实现、SQL、DDL、权限或workflow。

## 已执行与已审证据

此前受测HEAD99f46f89d1ac512d68360062e0f53276f31e6dd5，实际PR checkout02fcbecc4c7338f8cca2ed4a2e6fc1f364fe75dd，tree9690b8483a8b4ee5413725501aee6b19f43f1dc8与该HEAD相同，parents为精确main/head。
- F8 run38041195029/job114181613952：adapter69+SQLClock34=103，0failure/error/skip，BUILD SUCCESS。
- U02 run38041195085：Java114181614189编译/定向/全套434，0failure/error、1skip；Python114181614361：11+9+1 passed。
- U03 run38041195074/job114181614127：编译/定向/全套434，0failure/error、1skip。
- Source25来自未改main702e280f的合并后run38035233680/job114164180154，并非#408新跑。不能称128次精确HEAD新执行。全套重复不累加唯一覆盖，CD08门控临床验证skip不算通过。

#409（514f3dcd83c53c5877fa06358ab5a169fd4cb217）定向复审：
CAS零行及五种RESUME target异常BOUNDED_CLOSED_ON_REVIEWED_PR_TREE；真实probe1062传播BOUNDED_ACCEPTED；adapter原生DML1062尚OPEN。以上不是本次新增代码已通过或已合main。
F8EV-D01要求更新本记录；本版明确替换原“所有CI尚未执行/预计128”的滞后表述。

## 本次新增原生DML故障测试

测试专用NativeClaimConflictDataSource同时用于独立Spring context的JPA EMF、JpaTransactionManager和F8Adapter。所有connection操作委托真实MySQL连接；仅在adapter原生claim INSERT的executeUpdate边界，复用实际SQL及绑定参数在同一底层connection种入冲突行，然后实际执行adapter原PreparedStatement，捕获真实SQLException1062/23000并原样抛出。记录原statement执行次数、seed行数、connection ID、autoCommit=false、after_sentinel未到达；核验typed确定回滚、零claim/decision、fresh ABSENT、干净重试ACCEPTED。

不mock返回值、不构造假SQLException、不使用SIGNAL，不绕过guard、不取消锁、不降低隔离，不增加consumer权限；这是一笔事务的原生DML真实约束故障，不能称生产竞争writer测试。新配置仅由该测试显式创建context。

本次预期新增1次执行：adapter70+SQLClock34=104。此处仅新增测试状态PENDING_FRESH_CI；本轮结束后的精确head、checkout/tree、run/job及实测结论记录于#408 PR body，不把先前103项证据冒称新104项通过。所有既有69项保留。

## 剩余范围

EV-F8-02原生claim INSERT1062等待真实CI及复审；即使通过，也只关闭该单事务故障路径，不关闭不遵守guard的外部writer竞争认证。
EV-F8-01 outbox写拒绝OPEN/NOT_EXECUTED；FULL_CHECKLIST_VERIFICATION=NOT_FULLY_CLOSED；REAL_D5=NOT_READY。生产owner CA/APPLIED/P01、APPLY、effect、dispatch/outbox、真实网络/crash、Oracle均未认证。不是独立人员批准，没有merge授权。

## 本轮真实执行结果（代码提交5bd29b16）

受测代码HEAD：5bd29b16f54cdaed884229c2ace295e259e66395。实际checkout：f69a48c410c925dba6808a32c6a2e1d55cfdfe5a；parents main702e280f与代码HEAD；tree49a5154fd7fbda2a5114bd6daa5aeb88a3b1fdc6与代码HEAD完全一致。
- F8 [38191136807](https://github.com/cxjchelsea/AIdoctor/actions/runs/38191136807)，job114623947757：adapter70+SQLClock34=104，failure/error/skip均0，BUILD SUCCESS。
- U03 [38191136775](https://github.com/cxjchelsea/AIdoctor/actions/runs/38191136775)，job114623947585：编译/定向/全套434，0failure/error、1skip，BUILD SUCCESS。
- U02 [38191136786](https://github.com/cxjchelsea/AIdoctor/actions/runs/38191136786)，Java job114623948053同样434、0failure/error、1skip；Python job114623947857为11+9+1 passed。
三个run最终均completed/success；四份job日志核验checkout及汇总。原生INSERT日志有真实SQL、1062/23000；新增断言记录种入1行、原execute1次、未到after_sentinel、确定rollback/fresh ABSENT/重试成功。

NATIVE_CLAIM_INSERT_1062=VERIFIED_IN_SINGLE_TRANSACTION_FIXTURE；等待新增fixture定向复审，不冒充外部竞争writer认证。F8EV-D01已将历史/当前实测证据区分并记录到仓库；是否关闭以复审为准。上文PENDING描述为本次测试发布时状态，本节替代该状态。

本节作为仅文档提交发布：其HEAD不称为F8受测HEAD；F8测试代码blob与5bd29b16完全相同，继承上述代码证据。没有新代码修改，不需要将文档commit冒充104次新执行。Source25仍为main继承证据。REAL_D5/其余排除门禁不变，未合并。
