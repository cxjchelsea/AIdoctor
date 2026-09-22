# Foundation-0 实施与验证记录

> 状态：IMPLEMENTED / COMPONENT_VERIFIED / NOT_BUSINESS_WIRED
>
> 分支：`impl/foundation-0-runtime-base`
>
> PR：#77
>
> 本记录不构成 U01 Implementation Authorization、Merge Authorization、Clinical Runtime Enablement 或 Production Authorization。

## 1. 已实现范围

- FND-01：Consultation Runtime Authority / Strangler Binding
- FND-02：Canonical Business Event Identity Ledger
- FND-03：最小 Thread / Run 执行骨架
- FND-04：StateCommitter `StateRepositoryPort` → authoritative CDP Adapter
- FND-05：Scope / Capability Set / Contract Version Binding
- FND-06：Trace Runtime/Governance Correlation References
- MySQL / Oracle migration
- Foundation-0 与 StateCommitter 专项测试
- Foundation-0 独立 GitHub Actions 验证入口

## 2. 保持的不变量

1. 不建立第二套 Clinical Truth。
2. Runtime 只引用 Clinical State Version，不拥有 Clinical Truth。
3. Capability Result / Runtime Result 不能直接改写 Clinical Truth。
4. Canonical Event Ledger 只解决事件身份与 transport replay，不裁决业务 `ACCEPTED / DUPLICATE / EXPIRED / REJECTED / APPLIED`。
5. Legacy-bound Consultation 不能由 `CLINICAL_RUNTIME_V1` 打开 Run。
6. 当前 Contract / CDP 无法安全表达的临床 mutation fail-closed，不做 JSON stringify 绕过。
7. StateCommitter 机械核心继续保持 Spring/JPA 无关；真实 CDP Adapter 位于 Runtime/Infrastructure 侧。
8. 不引入 permissive/fake capability、consent、field permission、source validation production wiring。

## 3. 实施中发现并关闭的问题

### F0-R01 Shared Contracts API 兼容

初版 Adapter 错误按 reviewed `1.1.0` binding API 编写，而 `diagnosis-service` 当前真实依赖为 `shared-contracts:1.0.0`。

处理：Adapter 回归当前 diagnosis-service 实际 API；不在 Foundation-0 擅自升级 Contract。

状态：CLOSED。

### F0-R02 Runtime Binding 并发创建竞争

两个并发请求都可能先读取“binding 不存在”。

处理：依赖数据库唯一约束确定 winner，捕获并发唯一键竞争后重新读取 winner，并校验 immutable binding 完全一致；不一致则失败。

状态：CLOSED。

### F0-R03 StateCommitter 架构护栏失败

第一次可执行验证发现 CDP Adapter 位于 `state.committer.adapters`，被既有 Architecture Guard 正确识别为 Spring/JPA 污染。

处理：不削弱护栏；将 Adapter 与专项测试迁移到 `runtime.foundation.adapters`，机械 StateCommitter 包恢复纯净。

状态：CLOSED。

## 4. 可执行验证证据

新增 workflow：`.github/workflows/foundation-0-verification.yml`。

验证 Run：`34556206803`

验证代码 HEAD：`62ad38165abef6784eab5d9ed23e936b4561c011`

结果：

- Shared Contracts v1 Java binding install：PASS
- diagnosis-service compile：PASS
- Foundation-0 + StateCommitter focused tests：PASS
- diagnosis-service regression suite：PASS
- workflow job `java-foundation`：SUCCESS

因此可以确认本次 Foundation-0 代码达到 **COMPONENT_VERIFIED**。后续仅文档状态记录的提交不改变上述已验证代码树。

## 5. 尚未完成的接线

当前 `StateCommitter` 生产接线仍缺少完整、正式且非 permissive 的以下 Port 实现/来源：

- Capability Policy
- Field Permission
- Consent Policy
- Source Validation
- Idempotency
- Audit / Event Evidence 的生产级组合

Foundation-0 不应通过默认放行、测试 Fake 或临时 Bean 绕过这些授权链。因此本轮明确保持：

`StateCommitter + ClinicalCdpStateRepositoryAdapter = IMPLEMENTED / COMPONENT_VERIFIED / NOT_BUSINESS_WIRED`

该状态是安全边界：正式 wiring 必须由后续被授权 Unit 所需的真实 business/policy ownership 驱动，并继续经过独立审查与测试。

## 6. 当前结论

```text
Foundation-0 Code            = IMPLEMENTED
Foundation-0 Component Tests = VERIFIED
Architecture Guard           = PASS
Diagnosis Regression         = PASS
Clinical Mainline Wiring     = NOT_COMPLETE
Clinical Runtime             = NOT_ENABLED
U01                           = NOT_STARTED
U01 Authorization            = NOT_IMPLIED
Production                    = BLOCKED
Merge Authorization          = NOT_GRANTED
PR #77                        = OPEN / DRAFT / NOT_MERGED
```

在没有单独 U01 Implementation Authorization 前，不进入 U01 业务实现。
