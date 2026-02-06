# AI医生系统 - 错误处理规范

> **文档定位**：本文档定义AI医生系统的统一错误处理规范，包括异常类设计、错误码定义、全局异常处理等。  
> **参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》  
> **设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计

---

## 一、错误处理原则

### 1.1 基本原则

1. **统一响应格式**：所有错误都使用统一的响应格式
2. **明确的错误码**：每个错误都有唯一的错误码
3. **友好的错误信息**：错误信息对用户友好，对开发人员详细
4. **日志记录**：所有错误都要记录日志，便于排查问题
5. **安全考虑**：不暴露系统内部信息给用户
6. **可追溯性**：错误必须可关联到CDP、AgentState、请求链路（便于审计/回放/复盘）
7. **安全优先**：涉及高危/红旗/升级路径的错误，宁可保守（提示线下就医/升级处理），不能漏报
8. **工具调用错误处理**：工具执行失败、超时、部分成功等错误需要统一处理策略
9. **主Agent错误处理**：主Agent需要处理证据融合冲突、停止条件评估失败等错误

### 1.2 错误分类

**错误层级**：
- **工具层错误**：工具执行失败、超时、返回错误
- **服务层错误**：服务调用失败、服务不可用
- **系统层错误**：数据库连接失败、消息队列故障、网络故障
- **业务层错误**：CDP损坏、AgentState不一致、数据验证失败

**错误类型**：
- **客户端错误（4xx）**：请求参数错误、权限不足等
- **服务器错误（5xx）**：系统内部错误、服务不可用等
- **业务错误（自定义）**：业务逻辑错误，使用自定义错误码
- **依赖错误（5xx/503/504）**：数据库/Redis/Neo4j/下游服务/模型服务不可用或超时
- **并发/版本错误（409）**：CDP乐观锁/版本冲突、并发写冲突、幂等冲突
- **安全流程错误（4xx/5xx）**：红旗识别/风险分级/升级触发失败（按"宁可误报"兜底）
- **工具调用错误**：工具执行失败、超时、部分成功（status=failure/timeout/partial_success）
- **证据融合错误**：证据冲突、冲突解决失败
- **停止条件错误**：停止条件评估失败、决策错误

---

## 二、统一响应格式

### 2.1 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1705123456789
}
```

### 2.2 错误响应

```json
{
  "code": 400,
  "message": "请求参数验证失败",
  "data": null,
  "errors": [
    {
      "field": "userId",
      "message": "用户ID不能为空"
    }
  ],
  "traceId": "3f1a9b1d2c5a4b0e",
  "requestId": "req_20260122_0001",
  "service": "diagnosis-service",
  "agentId": "agent_main",
  "cdpId": "cdp_123456",
  "retryable": false,
  "timestamp": 1705123456789,
  "path": "/api/v1/diagnosis/start"
}
```

### 2.3 业务错误响应

```json
{
  "code": 2013,
  "message": "诊断记录不存在",
  "data": null,
  "traceId": "3f1a9b1d2c5a4b0e",
  "requestId": "req_20260122_0001",
  "service": "diagnosis-service",
  "agentId": "agent_main",
  "cdpId": "cdp_123456",
  "retryable": false,
  "timestamp": 1705123456789,
  "path": "/api/v1/diagnosis/diag_123456"
}
```

### 2.4 统一响应字段说明（新增）

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| `code` | 是 | 业务错误码（本规范定义） |
| `message` | 是 | 面向用户的简短错误描述（不可泄露内部信息） |
| `data` | 是 | 固定为 `null` |
| `errors` | 否 | 参数校验错误详情（字段级） |
| `traceId` | 是 | 链路追踪ID（跨服务一致） |
| `requestId` | 否 | 业务请求ID（便于定位一次用户交互） |
| `service` | 否 | 产生错误的服务名（如 `diagnosis-engine-service`） |
| `agentId` | 否 | 产生错误的主Agent或工具（如 `agent_main` / `tool_3`） |
| `cdpId` | 否 | 关联CDP ID（涉及CDP读写必须带） |
| `retryable` | 否 | 是否建议客户端/主Agent自动重试（true/false） |
| `timestamp` | 是 | 毫秒时间戳 |
| `path` | 是 | 请求路径 |

> **命名约定补充**：
> - **对外REST API响应**：字段使用 `camelCase`（本规范以上JSON示例为准）
> - **内部日志/事件/持久化**：可使用 `snake_case`（例如 `trace_id`、`cdp_id`）但不得影响对外API契约

---

## 三、错误码定义

### 3.1 HTTP状态码

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 请求成功处理 |
| 400 | 请求错误 | 参数验证失败、格式错误 |
| 401 | 未授权 | Token无效或过期 |
| 403 | 禁止访问 | 权限不足 |
| 404 | 资源不存在 | 请求的资源不存在 |
| 500 | 服务器错误 | 系统内部错误 |

### 3.2 业务错误码分段（重要：避免冲突，按服务/工具分配）

> **约束**：同一错误码在全系统内必须唯一；错误码段与微服务边界一致。  
> **命名**：尽量体现“服务 + 场景 + 原因”，避免同码多义。

| 范围 | 归属 | 说明 |
|------|------|------|
| 1000-1099 | health-state-assessment-service（tool_0） | 入口判定/健康状态判定/红旗兜底 |
| 1100-1199 | clinical-parsing-service（tool_1） | 概念识别/归一化/结构化提取/多模态 |
| 1200-1299 | dialog-service（tool_2） | 信息缺口/追问生成/对话上下文 |
| 1300-1399 | workup-planner-service（tool_4） | 信息增益/检查价值/验证计划 |
| 1400-1499 | treatment-engine-service（tool_5） | 处置建议/药物类别/生活方式 |
| 1500-1599 | risk-assessment-service（tool_6） | 风险分级/高危识别/升级规则 |
| 1600-1699 | explanation-service（tool_7） | 证据链/可解释/终点结论包 |
| 1700-1799 | CDP管理（跨服务通用） | CDP创建/更新/版本/回放/回填/回退 |
| 1800-1899 | A路径健康筛查（跨服务通用） | 需求分类/健康档案/分支执行/随访闭环 |
| 2000-2099 | diagnosis-service（主Agent服务，Java） | 流程编排/聚合/下游调用失败封装 |
| 2100-2199 | examination-service（Java） | 检查业务/报告上传/解析/OCR编排 |
| 3000-3999 | diagnosis-engine-service（tool_3） | 规则/知识图谱/融合/路径注入LLM |
| 4000-4999 | ocr-service（Python） | OCR识别/解析/置信度/格式 |
| 5000-5999 | 通用错误（跨服务） | 参数校验/格式/DB/缓存/外部依赖 |
| 6000-6099 | 并发与幂等（跨服务） | 乐观锁/版本冲突/幂等冲突 |
| 6100-6199 | 调用治理（跨服务） | 超时/熔断/限流/降级/重试耗尽 |

#### diagnosis-service（主Agent服务）错误码（2000-2099）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 2001 | 请求上下文缺失（如缺少cdpId/sessionId） | 400 |
| 2002 | 工作态无效或与流程阶段不匹配 | 400 |
| 2003 | 必填信息缺口未补齐，无法进入下一步 | 400 |
| 2004 | 流程正在执行中（幂等保护/重复提交） | 409 |
| 2005 | 流程已完成，禁止修改关键字段 | 400 |
| 2006 | 流程已取消 | 400 |
| 2007 | 工具服务调用失败（非超时） | 502 |
| 2008 | 工具服务调用超时 | 504 |
| 2009 | 工具服务不可用（熔断/健康检查失败） | 503 |
| 2010 | 证据融合失败（多工具证据冲突未收敛） | 500 |
| 2011 | 冲突解决失败 | 500 |
| 2012 | 消息队列投递失败（异步任务） | 503 |
| 2013 | CDP读写失败（封装/落库/缓存） | 500 |
| 2014 | AgentState更新失败 | 500 |
| 2015 | 停止条件评估失败 | 500 |
| 2016 | 升级策略执行失败 | 500 |
| 2017 | 拒答策略执行失败 | 500 |
| 2018 | 运行循环异常中断 | 500 |
| 2019 | 预算耗尽（max_tool_calls/max_time_seconds/max_cost） | 429 |
| 2020 | 诊断记录不存在 | 404 |

#### 工具调用错误码（2200-2299）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 2201 | 工具执行失败（status=failure） | 500 |
| 2202 | 工具执行超时（status=timeout） | 504 |
| 2203 | 工具返回部分成功（status=partial_success） | 200 |
| 2204 | 工具连续失败（同一工具连续失败3次以上） | 503 |
| 2205 | 工具调用参数验证失败 | 400 |
| 2206 | 工具返回结果格式错误 | 500 |
| 2207 | 工具evidence缺失或无效 | 400 |
| 2208 | 工具suggestedWrites格式错误 | 400 |
| 2209 | 工具quality指标异常 | 500 |
| 2210 | 工具降级策略执行失败 | 500 |

#### examination-service（检查服务，Java）错误码（2100-2199）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 2101 | 检查记录不存在 | 404 |
| 2102 | 文件格式不支持 | 400 |
| 2103 | 文件大小超限 | 400 |
| 2104 | 检查方案不存在 | 404 |
| 2105 | 检查类型无效 | 400 |
| 2106 | OCR识别失败（调用ocr-service失败） | 502 |
| 2107 | 文件解析失败 | 500 |
| 2108 | 检查结果解读失败 | 500 |

#### tool_0：健康状态判定工具错误码（1000-1099）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1001 | 健康状态判定失败 | 500 |
| 1002 | 症状严重程度评估失败 | 500 |
| 1003 | 风险早筛失败 | 500 |
| 1004 | 工作态判定失败 | 500 |
| 1005 | 红旗信号识别失败 | 500 |
| 1006 | 健康管理计划生成失败 | 500 |

#### tool_1：病例理解工具错误码（1100-1199）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1101 | 医学概念识别失败 | 500 |
| 1102 | 概念归一化失败 | 500 |
| 1103 | 多模态理解失败 | 500 |
| 1104 | 结构化提取失败 | 500 |
| 1105 | 歧义表达判定失败 | 500 |
| 1106 | OCR识别失败（tool_1调用） | 500 |

#### tool_2：主动问诊工具错误码（1200-1299）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1201 | 信息缺口识别失败 | 500 |
| 1202 | 智能追问生成失败 | 500 |
| 1203 | 自然语言理解失败（NLU） | 500 |
| 1204 | 自然语言生成失败（NLG） | 500 |
| 1205 | 对话上下文管理失败 | 500 |
| 1206 | 追问次数超限 | 400 |

#### tool_3：鉴别诊断工具错误码（3000-3999）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 3001 | 诊断引擎服务不可用 | 503 |
| 3002 | 诊断引擎调用超时 | 504 |
| 3003 | 规则引擎执行失败 | 500 |
| 3004 | 知识图谱查询失败 | 500 |
| 3005 | **知识图谱路径检索失败（DR.KNOWS核心）** | 500 |
| 3006 | **路径评分排序失败（DR.KNOWS核心）** | 500 |
| 3007 | **路径注入LLM失败（DR.KNOWS核心）** | 500 |
| 3008 | 统计模型推理失败 | 500 |
| 3009 | 大模型调用失败 | 500 |
| 3010 | 鉴别诊断引擎失败 | 500 |
| 3011 | 多引擎融合失败 | 500 |
| 3012 | 推理子组组织失败 | 500 |
| 3013 | 分流路径设计失败 | 500 |
| 3014 | 证据分析失败 | 500 |

#### tool_4：检查建议工具错误码（1300-1399）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1301 | 检查价值评估失败 | 500 |
| 1302 | 信息增益计算失败 | 500 |
| 1303 | 检查优先级排序失败 | 500 |
| 1304 | 验证计划生成失败 | 500 |

#### tool_5：治疗建议工具错误码（1400-1499）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1401 | 治疗方案推理失败 | 500 |
| 1402 | 药物推荐失败 | 500 |
| 1403 | 非药物治疗建议生成失败 | 500 |

#### tool_6：风险评估工具错误码（1500-1599）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1501 | 高危识别失败 | 500 |
| 1502 | 严重程度评估失败 | 500 |
| 1503 | 紧急程度分级失败 | 500 |
| 1504 | 复评与升级规则执行失败 | 500 |

#### tool_7：证据链工具错误码（1600-1699）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1601 | 证据链构建失败 | 500 |
| 1602 | 推理路径可视化失败 | 500 |
| 1603 | 证据来源标注失败 | 500 |
| 1604 | 终点结论包生成失败 | 500 |

#### 健康筛查流程（A路径）错误码（1800-1899）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1801 | 需求分类失败 | 500 |
| 1802 | 需求类型无效 | 400 |
| 1803 | 健康画像收集失败 | 500 |
| 1804 | 健康画像不完整 | 400 |
| 1805 | 必填信息缺失 | 400 |
| 1806 | 分支执行失败 | 500 |
| 1807 | 筛查建议生成失败 | 500 |
| 1809 | 健康目标管理失败 | 500 |
| 1810 | 计划性健康需求识别失败 | 500 |
| 1811 | 统一结果生成失败 | 500 |
| 1812 | 随访计划设置失败 | 500 |
| 1813 | 健康筛查记录不存在 | 404 |

#### CDP管理错误码（1700-1799）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 1701 | CDP创建失败 | 500 |
| 1702 | CDP更新失败 | 500 |
| 1703 | CDP版本控制失败 | 500 |
| 1704 | CDP不存在 | 404 |
| 1705 | CDP版本历史获取失败 | 500 |
| 1706 | CDP回放失败 | 500 |
| 1707 | CDP回填失败 | 500 |
| 1708 | CDP重排失败 | 500 |
| 1709 | CDP回退失败 | 500 |

#### OCR服务错误码（4000-4999）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 4001 | OCR识别失败 | 500 |
| 4002 | 文件解析失败 | 500 |
| 4003 | 图片格式不支持 | 400 |
| 4004 | 图片质量过低 | 400 |
| 4005 | 文字识别置信度过低 | 400 |

#### 通用错误码（5000-5999）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 5001 | 参数验证失败 | 400 |
| 5002 | 数据格式错误 | 400 |
| 5003 | 数据库操作失败 | 500 |
| 5004 | 缓存操作失败 | 500 |
| 5005 | 外部服务调用失败 | 500 |
| 5006 | 服务不可用 | 503 |

#### 并发与幂等错误码（6000-6099）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 6001 | CDP版本冲突（乐观锁失败） | 409 |
| 6002 | 幂等冲突（重复请求/重复回填） | 409 |
| 6003 | 并发写冲突（字段写锁失败/优先级冲突） | 409 |

#### 调用治理错误码（6100-6199）

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 6101 | 调用超时（通用） | 504 |
| 6102 | 重试耗尽 | 504 |
| 6103 | 熔断开启（拒绝调用） | 503 |
| 6104 | 限流触发 | 429 |
| 6105 | 降级执行（返回默认/空结果） | 200 |

---

## 四、异常类设计

### 4.1 基础异常类

```java
package com.aidoctor.diagnosis.exception;

import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    private final String message;
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}
```

### 4.2 诊断服务异常类

```java
package com.aidoctor.diagnosis.exception;

/**
 * 诊断记录不存在异常
 */
public class DiagnosisNotFoundException extends BusinessException {
    public DiagnosisNotFoundException(String diagnosisId) {
        super(2001, String.format("请求上下文缺失或诊断记录不存在: %s", diagnosisId));
    }
}

/**
 * 诊断状态不允许此操作异常
 */
public class DiagnosisStatusException extends BusinessException {
    public DiagnosisStatusException(String diagnosisId, String currentStatus, String requiredStatus) {
        super(2002, String.format("状态不允许此操作: 当前状态=%s, 需要状态=%s", 
            currentStatus, requiredStatus));
    }
}

/**
 * 信息完整度不足异常
 */
public class InsufficientInformationException extends BusinessException {
    public InsufficientInformationException(Double completeness, Double required) {
        super(2003, String.format("必填信息缺口未补齐: 当前=%.2f, 需要≥%.2f", 
            completeness, required));
    }
}

/**
 * 诊断分析中异常
 */
public class DiagnosisAnalyzingException extends BusinessException {
    public DiagnosisAnalyzingException(String diagnosisId) {
        super(2004, String.format("流程执行中，请稍候（幂等保护）: %s", diagnosisId));
    }
}
```

### 4.3 检查服务异常类

```java
package com.aidoctor.diagnosis.exception;

/**
 * 检查记录不存在异常
 */
public class ExaminationNotFoundException extends BusinessException {
    public ExaminationNotFoundException(Long examinationId) {
        super(2101, String.format("检查记录不存在: %d", examinationId));
    }
}

/**
 * 文件格式不支持异常
 */
public class UnsupportedFileFormatException extends BusinessException {
    public UnsupportedFileFormatException(String format) {
        super(2102, String.format("文件格式不支持: %s", format));
    }
}

/**
 * 文件大小超限异常
 */
public class FileSizeExceededException extends BusinessException {
    public FileSizeExceededException(Long size, Long maxSize) {
        super(2103, String.format("文件大小超限: 当前=%d, 最大=%d", size, maxSize));
    }
}
```

### 4.4 诊断引擎服务异常类

```java
package com.aidoctor.diagnosis.exception;

/**
 * 诊断引擎服务不可用异常
 */
public class DiagnosisEngineUnavailableException extends BusinessException {
    public DiagnosisEngineUnavailableException(String serviceName) {
        super(3001, String.format("诊断引擎服务不可用: %s", serviceName));
    }
}

/**
 * 诊断引擎调用超时异常
 */
public class DiagnosisEngineTimeoutException extends BusinessException {
    public DiagnosisEngineTimeoutException(String serviceName, Long timeout) {
        super(3002, String.format("诊断引擎调用超时: %s, 超时时间=%dms", serviceName, timeout));
    }
}
```

### 4.5 OCR服务异常类

```java
package com.aidoctor.diagnosis.exception;

/**
 * OCR识别失败异常
 */
public class OcrRecognitionException extends BusinessException {
    public OcrRecognitionException(String reason) {
        super(4001, String.format("OCR识别失败: %s", reason));
    }
}
```

---

## 五、全局异常处理器

### 5.1 Java服务全局异常处理器

```java
package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: code={}, message={}, path={}", 
            e.getCode(), e.getMessage(), request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.builder()
            .code(e.getCode())
            .message(e.getMessage())
            .data(null)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        HttpStatus httpStatus = getHttpStatus(e.getCode());
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("参数验证失败: path={}", request.getRequestURI());
        
        List<ApiResponse.ErrorDetail> errors = new ArrayList<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                errors.add(ApiResponse.ErrorDetail.builder()
                    .field(fieldError.getField())
                    .message(fieldError.getDefaultMessage())
                    .build());
            }
        });
        
        ApiResponse<Object> response = ApiResponse.builder()
            .code(5001)
            .message("参数验证失败")
            .data(null)
            .errors(errors)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理文件大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件大小超限: path={}", request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.builder()
            .code(2003)
            .message("文件大小超限")
            .data(null)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(
            Exception e, HttpServletRequest request) {
        log.error("系统异常: path={}", request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.builder()
            .code(500)
            .message("系统内部错误")
            .data(null)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 根据错误码获取HTTP状态码
     */
    private HttpStatus getHttpStatus(Integer code) {
        if (code >= 2000 && code < 2100) {
            // diagnosis-service（主Agent服务）
            if (code == 2004) return HttpStatus.CONFLICT;
            if (code == 2007) return HttpStatus.BAD_GATEWAY;
            if (code == 2008) return HttpStatus.GATEWAY_TIMEOUT;
            if (code == 2009) return HttpStatus.SERVICE_UNAVAILABLE;
            return HttpStatus.BAD_REQUEST;
        } else if (code >= 2100 && code < 2200) {
            // examination-service
            if (code == 2101 || code == 2104) return HttpStatus.NOT_FOUND;
            if (code == 2106) return HttpStatus.BAD_GATEWAY;
            return HttpStatus.BAD_REQUEST;
        } else if (code >= 3000 && code < 4000) {
            // 诊断引擎服务错误
            if (code == 3001) return HttpStatus.SERVICE_UNAVAILABLE;
            if (code == 3002) return HttpStatus.GATEWAY_TIMEOUT;
            return HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (code >= 4000 && code < 5000) {
            // OCR服务错误
            if (code == 4003 || code == 4004 || code == 4005) return HttpStatus.BAD_REQUEST;
            return HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (code >= 5000 && code < 6000) {
            // 通用错误
            if (code == 5001 || code == 5002) return HttpStatus.BAD_REQUEST;
            if (code == 5006) return HttpStatus.SERVICE_UNAVAILABLE;
            return HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (code >= 6000 && code < 6100) {
            return HttpStatus.CONFLICT;
        } else if (code >= 6100 && code < 6200) {
            if (code == 6104) return HttpStatus.TOO_MANY_REQUESTS;
            if (code == 6105) return HttpStatus.OK;
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
```

### 5.2 统一响应类

```java
package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 统一API响应
 */
@Data
@Builder
public class ApiResponse<T> {
    
    private Integer code;
    private String message;
    private T data;
    private List<ErrorDetail> errors;
    private Long timestamp;
    private String path;
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .code(200)
            .message("success")
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }
    
    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
            .code(code)
            .message(message)
            .data(null)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    @Data
    @Builder
    public static class ErrorDetail {
        private String field;
        private String message;
    }
}
```

---

## 六、Python服务异常处理

### 6.1 FastAPI异常处理

```python
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
import logging

logger = logging.getLogger(__name__)

app = FastAPI()

class BusinessException(Exception):
    """业务异常基类"""
    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message
        super().__init__(message)

class DiagnosisEngineUnavailableException(BusinessException):
    """诊断引擎服务不可用"""
    def __init__(self, service_name: str):
        super().__init__(3001, f"诊断引擎服务不可用: {service_name}")

class DiagnosisEngineTimeoutException(BusinessException):
    """诊断引擎调用超时"""
    def __init__(self, service_name: str, timeout: int):
        super().__init__(3002, f"诊断引擎调用超时: {service_name}, 超时时间={timeout}ms")

@app.exception_handler(BusinessException)
async def business_exception_handler(request: Request, exc: BusinessException):
    """处理业务异常"""
    logger.warning(f"业务异常: code={exc.code}, message={exc.message}, path={request.url.path}")
    
    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,  # 业务错误默认400；如需按码映射可扩展
        content={
            "code": exc.code,
            "message": exc.message,
            "data": None,
            "traceId": request.headers.get("x-trace-id"),
            "service": "diagnosis-engine-service",
            "agentId": "agent_3",
            "retryable": False,
            "timestamp": int(time.time() * 1000),
            "path": str(request.url.path)
        }
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """处理参数验证异常"""
    logger.warning(f"参数验证失败: path={request.url.path}")
    
    errors = []
    for error in exc.errors():
        errors.append({
            "field": ".".join(str(loc) for loc in error["loc"]),
            "message": error["msg"]
        })
    
    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={
            "code": 5001,
            "message": "参数验证失败",
            "data": None,
            "errors": errors,
            "timestamp": int(time.time() * 1000),
            "path": str(request.url.path)
        }
    )

@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """处理未知异常"""
    logger.error(f"系统异常: path={request.url.path}", exc_info=True)
    
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "code": 500,
            "message": "系统内部错误",
            "data": None,
            "timestamp": int(time.time() * 1000),
            "path": str(request.url.path)
        }
    )
```

---

## 七、日志记录规范

### 7.1 日志级别

- **ERROR**：系统错误、异常
- **WARN**：业务异常、警告
- **INFO**：关键业务流程
- **DEBUG**：详细调试信息

### 7.2 日志格式

```java
// 错误日志
log.error("诊断分析失败: diagnosisId={}, error={}", diagnosisId, e.getMessage(), e);

// 警告日志
log.warn("信息完整度不足: diagnosisId={}, completeness={}", diagnosisId, completeness);

// 信息日志
log.info("开始诊断分析: diagnosisId={}, userId={}", diagnosisId, userId);

// 调试日志
log.debug("诊断引擎调用: engine={}, request={}", engineType, request);
```

### 7.3 日志内容要求

- 包含关键业务标识（diagnosisId、userId等）
- 包含链路标识（traceId/requestId），跨服务可串联
- 包含CDP标识（cdpId/version）与智能体标识（agentId）
- 包含错误原因和上下文信息
- 不记录敏感信息（密码、Token等）
- 异常要记录完整堆栈

---

## 八、跨服务调用与超时重试策略（与技术架构对齐，新增）

> **目标**：让“错误处理”与“编排/多服务调用/安全兜底”一致，避免只写异常类、不写系统性策略。

### 8.1 调用超时与重试（默认值）

| 服务 | 超时时间 | 重试次数 | 重试间隔 | 失败建议 |
|------|----------|----------|----------|----------|
| health-state-assessment-service | 5秒 | 2次 | 1秒 | 失败则保守进入临床诊疗态或提示线下就医 |
| clinical-parsing-service | 10秒 | 2次 | 2秒 | 返回“信息不足”并触发补问/降级模板 |
| diagnosis-engine-service | 30秒 | 1次 | 5秒 | 返回“不可确证”与下一步建议，不给确诊 |
| workup-planner-service | 15秒 | 1次 | 3秒 | 降级为空计划或仅输出“就医/检查建议” |
| treatment-engine-service | 15秒 | 1次 | 3秒 | 降级为通用对症与随访建议 |
| risk-assessment-service | 10秒 | 2次 | 2秒 | 风险兜底：宁可提高风险等级，提示就医 |
| explanation-service | 20秒 | 1次 | 3秒 | 降级为简版解释（不阻塞主流程） |

### 8.2 熔断与降级（建议）

- **高危优先**：风险评估/红旗相关调用失败时，不降级为“安全”，而是降级为“保守提示就医/升级处理”。
- **解释可降级**：解释生成失败不应阻塞流程输出，可返回简版结论包（保留关键依据与行动）。
- **检查/治疗可降级**：workup/treatment失败时可返回“推荐线下就医/基础检查清单”，并标记`retryable=true`给异步补全。

### 8.3 与CDP版本/回退机制的关系

- **CDP更新失败**：优先返回 `6001/1702` 并提示"请重试"；主Agent可按幂等键重放。
- **证据冲突/回填失败**：返回 `1707/1708/1709` 并触发回退到 Step 4 或 Step 3（按回退条件表）。

---

## 九、错误处理最佳实践

### 8.1 异常处理原则

1. **尽早捕获**：在业务逻辑层捕获异常
2. **明确分类**：区分业务异常和系统异常
3. **友好提示**：给用户友好的错误提示
4. **详细日志**：记录详细的错误信息供排查

### 8.2 异常处理示例

```java
@Service
public class DiagnosisService {
    
    public DiagnosisResponse startDiagnosis(DiagnosisRequest request) {
        try {
            // 1. 验证参数
            validateRequest(request);
            
            // 2. 创建诊断记录
            DiagnosisRecord record = createDiagnosisRecord(request);
            
            // 3. 获取健康档案
            HealthProfile profile = profileService.getHealthProfile(request.getUserId());
            if (profile == null) {
                throw new BusinessException(5006, "健康档案服务不可用");
            }
            
            // 4. 检查信息完整度
            double completeness = calculateCompleteness(record, profile);
            if (completeness < 0.6) {
                throw new InsufficientInformationException(completeness, 0.6);
            }
            
            // 5. 执行诊断
            return analyze(record, profile);
            
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 系统异常包装后抛出
            log.error("诊断服务异常: userId={}", request.getUserId(), e);
            throw new BusinessException(500, "诊断服务异常，请稍后重试");
        }
    }
}
```

---

## 十、错误码维护

### 9.1 错误码管理

- 错误码统一管理在常量类中
- 错误码文档及时更新
- 新增错误码需要评审

### 9.2 错误码常量类

```java
package com.aidoctor.diagnosis.constant;

/**
 * 错误码常量
 */
public class ErrorCode {
    
    // diagnosis-service（协调器）
    public static final int ORCH_CONTEXT_MISSING = 2001;
    public static final int ORCH_INVALID_WORK_MODE = 2002;
    public static final int ORCH_REQUIRED_GAP_NOT_FILLED = 2003;
    public static final int ORCH_IN_PROGRESS = 2004;
    
    // examination-service
    public static final int EXAMINATION_NOT_FOUND = 2101;
    public static final int UNSUPPORTED_FILE_FORMAT = 2102;
    public static final int FILE_SIZE_EXCEEDED = 2103;
    
    // 诊断引擎服务错误码
    public static final int ENGINE_UNAVAILABLE = 3001;
    public static final int ENGINE_TIMEOUT = 3002;
    
    // OCR服务错误码
    public static final int OCR_RECOGNITION_FAILED = 4001;
    
    // 通用错误码
    public static final int VALIDATION_FAILED = 5001;
    public static final int INTERNAL_ERROR = 500;
    // 并发与幂等
    public static final int CDP_VERSION_CONFLICT = 6001;
}
```

---

**文档版本**：v4.0（对齐“多智能体 + 多微服务 + CDP版本管理/回退机制 + 调用治理”）  
**创建日期**：2025年1月  
**更新日期**：2026年1月  
**文档定位**：AI医生系统的错误处理规范（异常类、错误码、全局异常处理）  
**参考文档**：《AI医生系统-系统功能设计.md》、《AI医生系统-技术架构设计.md》  
**设计基础**：基于DR.KNOWS论文，采用单主Agent + 多工具Tools架构设计  
**更新说明**：修正错误码分段冲突（examination-service改为2100段），补齐主Agent服务/并发/调用治理错误码；统一错误响应契约字段（traceId/cdpId/agentId/retryable）；对齐跨服务调用超时重试与降级策略；修正Python业务异常不再统一返回HTTP 200。

