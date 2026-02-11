# HPO数据提取脚本使用说明

## 文件说明

- `extract_hpo_data.py` - HPO数据提取脚本
- `hp.owl` - HPO源数据文件（72MB）
- `hpo_data.json` - 提取后的JSON数据文件（运行脚本后生成）

## 使用方法

### 1. 环境要求

- Python 3.7+
- 使用Python标准库，无需额外依赖

### 2. 运行脚本

```bash
cd docs/资料
python extract_hpo_data.py
```

### 3. 输出文件

脚本会在同目录下生成 `hpo_data.json` 文件，包含：

- `metadata` - 元数据（版本、提取日期、统计信息）
- `terms` - 所有HPO术语数据
- `synonym_index` - 同义词索引（用于快速查找）
- `alternative_id_mapping` - 替代ID映射
- `deprecated_terms` - 废弃术语列表

## 提取的字段

### 必须提取（核心功能）
- ✅ `id` - 术语标识符
- ✅ `label` - 标准名称
- ✅ `exact_synonyms` - 精确同义词
- ✅ `related_synonyms` - 相关同义词
- ✅ `parent_ids` - 父类关系
- ✅ `deprecated` - 废弃标记

### 建议提取（重要功能）
- ✅ `definition` - 定义
- ✅ `child_ids` - 子类关系（计算得出）
- ✅ `is_leaf` - 叶子节点标记（计算得出）
- ✅ `alternative_ids` - 替代ID
- ✅ `replaced_by` - 被替代术语
- ✅ `xrefs` - 外部引用

### 可选提取（辅助功能）
- ✅ `comment` - 注释
- ✅ `creation_date` - 创建日期

## 处理特性

1. **支持owl:Axiom** - 自动提取带注解的属性（如同义词、定义等）
2. **处理Restriction** - 正确解析复杂的父类关系
3. **自动计算派生字段** - child_ids、is_leaf等
4. **构建索引** - 自动生成同义词索引和替代ID映射
5. **数据验证** - 提取后自动验证数据完整性

## 性能说明

- 文件大小：72MB
- 预计处理时间：1-3分钟（取决于机器性能）
- 内存占用：约200-300MB

## 注意事项

1. 确保 `hp.owl` 文件在同一目录下
2. 输出文件会覆盖同名的现有文件
3. 如果遇到内存问题，可以考虑使用流式解析（需要修改代码）

## 故障排除

### 问题：找不到文件
- 确保 `hp.owl` 在 `docs/资料/` 目录下
- 检查文件路径是否正确

### 问题：内存不足
- 72MB的文件通常不会导致内存问题
- 如果确实遇到，可以考虑分批处理或使用流式解析

### 问题：提取的术语数量不对
- 检查OWL文件是否完整
- 查看脚本输出的日志信息
- 检查是否有错误信息

