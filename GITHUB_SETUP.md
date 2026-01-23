# GitHub 仓库设置指南

本文档列出了在推送到 GitHub 之前需要完成的设置步骤。

## ✅ 已完成的工作

- ✅ 优化了 `.gitignore` 文件
- ✅ 优化了 `README.md` 文件
- ✅ 创建了 `LICENSE` 文件（MIT License）
- ✅ 创建了 GitHub Issue 模板（Bug 报告、功能请求）
- ✅ 创建了 Pull Request 模板
- ✅ 创建了 `CONTRIBUTING.md` 贡献指南
- ✅ 创建了 `SECURITY.md` 安全策略

## ✅ 已完成的替换

所有 GitHub 仓库地址占位符已替换为：`https://github.com/cxjchelsea/AIdoctor`

### 已替换的文件

1. **README.md**
   - ✅ 克隆地址：`https://github.com/cxjchelsea/AIdoctor.git`
   - ✅ Issues 链接：`https://github.com/cxjchelsea/AIdoctor/issues`
   - ✅ 已删除邮箱地址（如需添加，请手动添加）

2. **CONTRIBUTING.md**
   - ✅ 所有 Issues 链接已更新
   - ✅ 所有仓库地址已更新

3. **GITHUB_SETUP.md**
   - ✅ 推送步骤中的仓库地址已更新

## 🔧 可选更新内容

### SECURITY.md 中的占位符（可选）

**文件**: `SECURITY.md`

- ⚠️ 第 19 行：`security@example.com` → 如需接收安全报告，请替换为您的安全联系邮箱
- ⚠️ 第 72 行：`security@example.com` → 如需接收安全报告，请替换为您的安全联系邮箱

> **注意**：如果不需要接收安全报告，可以保留占位符或删除相关行。

### README.md 中的致谢部分（可选）

**文件**: `README.md`

- ⚠️ 第 389 行：`https://github.com/your-repo/DRKnows` → 如果 DR.KNOWS 有公开仓库，替换为实际地址；否则可以删除或注释掉

## 📋 推送前的检查清单

在推送到 GitHub 之前，请确保：

- [x] ✅ 已替换所有 README.md 中的 GitHub 地址占位符
- [x] ✅ 已替换所有 CONTRIBUTING.md 中的 GitHub 地址占位符
- [ ] ⚠️ 可选：替换 SECURITY.md 中的邮箱地址（如需要）
- [ ] ⚠️ 可选：更新 README.md 中 DR.KNOWS 的链接（如有公开仓库）
- [x] ✅ 检查 `.gitignore` 是否已正确忽略敏感文件
- [ ] ⚠️ 确认没有提交敏感信息（密码、API 密钥等）
- [ ] ⚠️ 检查 `application-mysql.yml` 和 `application-dev.yml` 中的密码是否为示例值（非生产密码）

## 🚀 推送步骤

### 1. 初始化 Git 仓库（如果还没有）

```bash
git init
```

### 2. 添加远程仓库

```bash
git remote add origin https://github.com/cxjchelsea/AIdoctor.git
```

### 3. 添加所有文件

```bash
git add .
```

### 4. 提交更改

```bash
git commit -m "feat: 初始提交 - AI医生智能诊断系统"
```

### 5. 推送到 GitHub

```bash
git branch -M main
git push -u origin main
```

## 🔒 安全建议

1. **检查敏感文件**：确保所有 `.env` 文件、包含密码的配置文件已被 `.gitignore` 忽略
2. **使用环境变量**：生产环境使用环境变量而非硬编码配置
3. **审查提交历史**：如果之前提交过敏感信息，使用 `git filter-branch` 或 `git-filter-repo` 清理历史

## 📝 后续工作（可选）

1. **设置 GitHub Actions**：添加 CI/CD 工作流
2. **添加代码质量检查**：集成 CodeQL、SonarCloud 等
3. **设置分支保护规则**：保护 main 分支
4. **添加项目徽章**：在 README 中添加更多徽章（CI 状态、代码覆盖率等）

## ❓ 需要帮助？

如果在设置过程中遇到问题，请参考：
- [GitHub 文档](https://docs.github.com/)
- [Git 文档](https://git-scm.com/doc)

---

祝您推送顺利！🎉

