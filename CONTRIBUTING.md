# 贡献指南

感谢您对 AI医生智能诊断系统 项目的关注！我们欢迎所有形式的贡献。

## 🤝 如何贡献

### 报告 Bug

如果您发现了 bug，请通过以下方式报告：

1. 检查 [Issues](https://github.com/cxjchelsea/AIdoctor/issues) 中是否已有相关报告
2. 如果没有，请创建一个新的 [Bug 报告](https://github.com/cxjchelsea/AIdoctor/issues/new?template=bug_report.md)
3. 提供尽可能详细的信息，包括：
   - 复现步骤
   - 期望行为
   - 实际行为
   - 环境信息（操作系统、Python/Java 版本等）
   - 相关日志或截图

### 提出新功能

如果您有功能建议：

1. 检查 [Issues](https://github.com/cxjchelsea/AIdoctor/issues) 中是否已有相关讨论
2. 如果没有，请创建一个新的 [功能请求](https://github.com/cxjchelsea/AIdoctor/issues/new?template=feature_request.md)
3. 详细描述功能需求和使用场景

### 提交代码

#### 1. Fork 项目

点击 GitHub 上的 "Fork" 按钮，将项目 fork 到您的账户。

#### 2. 克隆您的 Fork

```bash
git clone https://github.com/cxjchelsea/AIdoctor.git
cd AIdoctor
```

#### 3. 添加上游仓库

```bash
git remote add upstream https://github.com/cxjchelsea/AIdoctor.git
```

#### 4. 创建分支

```bash
git checkout -b feature/your-feature-name
# 或
git checkout -b fix/your-bug-fix
```

#### 5. 进行更改

- 遵循项目的代码规范
- 添加必要的测试
- 更新相关文档
- 确保所有测试通过

#### 6. 提交更改

使用清晰的提交信息，遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```bash
git commit -m "feat: 添加新功能描述"
# 或
git commit -m "fix: 修复bug描述"
```

提交类型：
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更改
- `style`: 代码格式（不影响代码含义）
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

#### 7. 推送更改

```bash
git push origin feature/your-feature-name
```

#### 8. 创建 Pull Request

1. 在 GitHub 上创建 Pull Request
2. 填写 PR 模板中的所有信息
3. 等待代码审查

## 📋 代码规范

### Python 代码规范

- 遵循 [PEP 8](https://www.python.org/dev/peps/pep-0008/) 规范
- 使用类型提示（Type Hints）
- 编写文档字符串（Docstrings）
- 行长度限制：88 字符（Black 默认）

```python
def example_function(param1: str, param2: int) -> bool:
    """
    函数描述
    
    Args:
        param1: 参数1描述
        param2: 参数2描述
    
    Returns:
        返回值描述
    """
    pass
```

### Java 代码规范

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 使用有意义的变量和方法名
- 添加必要的注释和 JavaDoc

```java
/**
 * 类描述
 */
public class ExampleClass {
    /**
     * 方法描述
     * @param param 参数描述
     * @return 返回值描述
     */
    public String exampleMethod(String param) {
        return param;
    }
}
```

### TypeScript/React 代码规范

- 遵循 ESLint 规则
- 使用 TypeScript 类型定义
- 组件使用函数式组件和 Hooks
- 遵循 React 最佳实践

```typescript
interface Props {
  title: string;
}

const ExampleComponent: React.FC<Props> = ({ title }) => {
  return <div>{title}</div>;
};
```

## 🧪 测试

在提交 PR 之前，请确保：

- [ ] 所有现有测试通过
- [ ] 为新功能添加了测试
- [ ] 测试覆盖率达到要求
- [ ] 代码通过 lint 检查

运行测试：

```bash
# Python 服务
cd <service-name>
pytest

# Java 服务
cd <service-name>
mvn test

# 前端
cd frontend
npm test
```

## 📚 文档

- 更新相关 README 文件
- 添加或更新 API 文档
- 更新 CHANGELOG（如果适用）

## 🔍 代码审查流程

1. 提交 PR 后，维护者会进行代码审查
2. 根据反馈进行修改
3. 审查通过后，代码将被合并

## ❓ 需要帮助？

如果您在贡献过程中遇到问题：

- 查看 [文档](./docs/)
- 提交 [Issue](https://github.com/cxjchelsea/AIdoctor/issues)
- 联系维护者

再次感谢您的贡献！🎉

