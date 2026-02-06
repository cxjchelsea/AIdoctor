# Git 提交信息规范

本文档定义了项目中使用的 Git 提交信息规范，遵循 [Conventional Commits](https://www.conventionalcommits.org/) 标准。

## 提交信息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 格式说明

- **type（必需）**：提交类型
- **scope（可选）**：影响范围，如模块名、服务名
- **subject（必需）**：简短描述，不超过 50 个字符
- **body（可选）**：详细描述，说明修改的动机和与之前行为的对比
- **footer（可选）**：关闭的 Issue 或 Breaking Changes

## 提交类型（Type）

### 主要类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **feat** | 新功能 | `feat: 添加用户登录功能` |
| **fix** | 修复 bug | `fix: 修复登录页面验证码不显示的问题` |
| **docs** | 文档变更 | `docs: 更新 API 文档` |
| **style** | 代码格式（不影响代码运行的变动） | `style: 格式化代码缩进` |
| **refactor** | 重构（既不是新增功能，也不是修复 bug） | `refactor: 重构用户服务层代码` |
| **perf** | 性能优化 | `perf: 优化数据库查询性能` |
| **test** | 测试相关 | `test: 添加用户登录单元测试` |
| **chore** | 构建过程或辅助工具的变动 | `chore: 更新依赖包版本` |
| **ci** | CI 配置文件和脚本的变更 | `ci: 配置 GitHub Actions 工作流` |
| **build** | 构建系统或外部依赖的变更 | `build: 更新 webpack 配置` |
| **revert** | 回滚之前的提交 | `revert: 回滚 feat: 添加新功能` |

### 类型详细说明

#### feat - 新功能
用于提交新功能的代码变更。

```bash
feat: 添加用户注册功能
feat(诊断服务): 实现诊断结果导出功能
feat(前端): 添加暗色主题切换
```

#### fix - 修复 bug
用于提交修复 bug 的代码变更。

```bash
fix: 修复登录验证逻辑错误
fix(API): 修复分页查询边界条件问题
fix(前端): 修复移动端样式显示异常
```

#### docs - 文档变更
用于提交文档相关的变更，包括 README、API 文档、注释等。

```bash
docs: 更新项目 README
docs(API): 补充接口文档说明
docs: 修正代码注释中的拼写错误
```

#### style - 代码格式
用于提交代码格式化的变更，不影响代码功能。

```bash
style: 统一代码缩进为 4 个空格
style(Python): 使用 black 格式化代码
style: 移除未使用的导入语句
```

#### refactor - 重构
用于提交代码重构的变更，不改变功能，但可能改变代码结构。

```bash
refactor: 重构用户认证模块
refactor(服务层): 提取公共方法减少代码重复
refactor: 优化异常处理逻辑
```

#### perf - 性能优化
用于提交性能优化的代码变更。

```bash
perf: 优化数据库查询性能
perf(缓存): 添加 Redis 缓存提升响应速度
perf: 减少不必要的 API 调用
```

#### test - 测试相关
用于提交测试相关的代码变更。

```bash
test: 添加用户服务单元测试
test(集成): 添加 API 集成测试用例
test: 修复测试用例中的断言错误
```

#### chore - 杂项
用于提交构建过程、工具配置等杂项变更。

```bash
chore: 更新依赖包版本
chore: 配置 ESLint 规则
chore: 更新 .gitignore 文件
```

#### ci - CI/CD 配置
用于提交持续集成/持续部署相关的配置变更。

```bash
ci: 配置 GitHub Actions 工作流
ci: 添加自动化测试流程
ci: 更新 Docker 构建配置
```

#### build - 构建系统
用于提交构建系统或外部依赖的变更。

```bash
build: 更新 webpack 配置
build: 升级 Node.js 版本要求
build: 配置 TypeScript 编译选项
```

#### revert - 回滚提交
用于回滚之前的提交。

```bash
revert: 回滚 feat: 添加新功能
revert(abc123): 回滚之前的提交
```

## 作用域（Scope）

作用域用于说明提交影响的范围，可以是：

- **服务名称**：`diagnosis-service`、`dialog-service`、`explanation-service`
- **模块名称**：`api`、`models`、`services`、`utils`
- **功能模块**：`用户模块`、`诊断模块`、`对话模块`

### 示例

```bash
feat(诊断服务): 添加诊断结果缓存
fix(前端-用户模块): 修复用户信息显示问题
docs(API): 更新接口文档
refactor(对话服务-意图识别): 优化意图识别算法
```

## 提交信息示例

### 简单提交

```bash
feat: 添加用户登录功能
fix: 修复登录验证码不显示的问题
docs: 更新 README 文档
```

### 带作用域的提交

```bash
feat(诊断服务): 实现诊断结果导出功能
fix(前端): 修复移动端样式显示异常
refactor(API): 重构用户认证逻辑
```

### 带详细描述的提交

```bash
feat(诊断服务): 添加诊断结果导出功能

- 支持导出为 PDF 格式
- 支持导出为 Excel 格式
- 添加导出历史记录功能

Closes #123
```

### 带 Breaking Changes 的提交

```bash
feat(API): 重构用户认证接口

BREAKING CHANGE: 用户认证接口路径从 /auth/login 改为 /api/v2/auth/login
旧的接口将在下个版本中移除，请及时更新客户端代码。
```

### 多行提交信息

```bash
fix(诊断服务): 修复诊断结果计算错误

修复了在特定条件下诊断结果计算不准确的问题。
主要变更：
- 修正了概率计算公式
- 添加了边界条件检查
- 更新了相关单元测试

Fixes #456
```

## 最佳实践

### 1. 提交信息规范

- ✅ **使用中文**：提交信息使用中文描述，便于团队理解
- ✅ **简洁明确**：subject 部分不超过 50 个字符，清晰描述做了什么
- ✅ **使用祈使句**：使用"添加"、"修复"、"更新"等动词开头
- ✅ **首字母小写**：subject 首字母不需要大写
- ✅ **不加句号**：subject 末尾不需要加句号

### 2. 提交粒度

- ✅ **一次提交一个功能**：每个提交应该只包含一个逻辑变更
- ✅ **小而频繁**：频繁提交小的变更，而不是积累大量变更后一次性提交
- ✅ **原子性提交**：确保每次提交后代码可以正常编译和运行

### 3. 提交类型选择

- ✅ **feat vs fix**：新功能用 `feat`，修复问题用 `fix`
- ✅ **refactor vs fix**：重构用 `refactor`，修复 bug 用 `fix`
- ✅ **style vs refactor**：仅格式化用 `style`，改变结构用 `refactor`
- ✅ **chore vs build**：工具配置用 `chore`，构建系统用 `build`

### 4. 作用域使用

- ✅ **明确作用域**：当提交影响特定模块时，使用作用域
- ✅ **保持一致性**：团队内使用统一的作用域命名
- ✅ **可省略**：如果变更影响多个模块或整个项目，可以省略作用域

## 常见提交示例

### 功能开发

```bash
feat(诊断服务): 添加诊断结果缓存机制
feat(前端): 实现诊断历史记录页面
feat(对话服务): 支持多轮对话上下文管理
```

### Bug 修复

```bash
fix(诊断服务): 修复诊断结果计算精度问题
fix(前端): 修复移动端页面布局错乱
fix(API): 修复分页查询边界条件错误
```

### 文档更新

```bash
docs: 更新项目 README 说明
docs(API): 补充接口文档参数说明
docs(架构): 更新系统架构设计文档
```

### 代码重构

```bash
refactor(诊断服务): 重构诊断逻辑处理流程
refactor(前端): 提取公共组件减少代码重复
refactor(API): 优化异常处理机制
```

### 性能优化

```bash
perf(诊断服务): 优化数据库查询性能
perf(前端): 使用虚拟滚动优化长列表渲染
perf(API): 添加 Redis 缓存减少数据库压力
```

### 测试相关

```bash
test(诊断服务): 添加诊断逻辑单元测试
test(集成): 添加 API 集成测试用例
test: 修复测试用例中的断言错误
```

### 工具配置

```bash
chore: 更新 Python 依赖包版本
chore: 配置 ESLint 代码检查规则
chore: 更新 .gitignore 忽略规则
```

## 工具支持

### Commitizen

使用 [Commitizen](https://github.com/commitizen/cz-cli) 可以交互式生成符合规范的提交信息：

```bash
npm install -g commitizen
npm install -D cz-conventional-changelog
```

### Commitlint

使用 [commitlint](https://github.com/conventional-changelog/commitlint) 可以自动检查提交信息是否符合规范：

```bash
npm install -D @commitlint/cli @commitlint/config-conventional
```

### Git Hooks

可以配置 Git hooks 在提交前自动检查提交信息格式。

## 版本发布

遵循此规范后，可以使用工具自动生成 CHANGELOG 和版本号：

- **CHANGELOG**：根据提交信息自动生成变更日志
- **版本号**：根据提交类型自动确定版本号增量
  - `feat` → 次版本号（minor）
  - `fix` → 修订版本号（patch）
  - `BREAKING CHANGE` → 主版本号（major）

## 参考资源

- [Conventional Commits 规范](https://www.conventionalcommits.org/)
- [Angular 提交规范](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit)
- [Commitizen](https://github.com/commitizen/cz-cli)

## 更新记录

- 2026-01-28：创建 Git 提交规范文档

