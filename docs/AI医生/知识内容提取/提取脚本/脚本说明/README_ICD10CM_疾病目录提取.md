# ICD-10-CM 疾病目录提取脚本说明

## 功能概述

该脚本用于从ICD-10-CM 2026官方XML数据文件中提取疾病目录，生成标准化的疾病分类表。

## 输入文件

脚本需要以下XML文件（位于 `源数据/icd10cm-table and index-2026/` 目录）：

1. **`icd10cm-index-2026.xml`** - 疾病索引文件（按字母顺序）
2. **`icd10cm-tabular-2026.xml`** - 疾病表格列表文件（按章节分类）

## 输出文件

脚本会在 `提取结果/` 目录下生成：

1. **`icd10cm_disease_catalog.json`** - 疾病目录（JSON格式）
2. **`icd10cm_disease_catalog.csv`** - 疾病目录（CSV格式，可用Excel打开）
3. **`icd10cm_disease_catalog_summary.json`** - 统计摘要

## 数据字段说明

每个疾病条目包含以下字段：

| 字段名 | 说明 | 示例 |
|--------|------|------|
| `standard_name` | 标准疾病名称（主键） | "Cholera" |
| `icd10cm_code` | ICD-10-CM编码 | "A00.0" |
| `aliases` | 别名列表 | ["霍乱", "Cholera"] |
| `chapter` | 所属章节编号 | "1" |
| `chapter_desc` | 章节描述 | "Certain infectious and parasitic diseases (A00-B99)" |
| `section` | 所属节ID | "A00-A09" |
| `section_desc` | 节的描述 | "Intestinal infectious diseases" |
| `full_path` | 完整分类路径 | "第1章 > Certain infectious... > Intestinal infectious diseases > Cholera" |
| `source` | 数据来源 | "index/tabular" 或 "index" 或 "tabular" |

## 使用方法

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 运行脚本

```bash
python extract_icd10cm_disease_catalog.py
```

### 3. 查看结果

- **JSON格式**：适合程序处理
- **CSV格式**：可用Excel打开，方便人工查看和编辑
- **摘要文件**：包含统计信息

## 输出示例

### JSON格式

```json
{
  "version": "2026",
  "source": "ICD-10-CM",
  "total_diseases": 85000,
  "diseases": [
    {
      "standard_name": "Cholera",
      "icd10cm_code": "A00.0",
      "aliases": ["Cholera", "Cholera due to Vibrio cholerae 01, biovar cholerae"],
      "chapter": "1",
      "chapter_desc": "Certain infectious and parasitic diseases (A00-B99)",
      "section": "A00-A09",
      "section_desc": "Intestinal infectious diseases",
      "full_path": "第1章 > Certain infectious and parasitic diseases (A00-B99) > Intestinal infectious diseases > Cholera",
      "source": "index/tabular"
    }
  ]
}
```

### CSV格式

可以用Excel打开，包含所有字段，方便：
- 人工筛选和查看
- 导出为其他格式
- 添加自定义字段（如中文名称）

## 在AI医生系统中的应用

### 1. 疾病目录构建

疾病目录是知识内容提取的"锚点"，用于：
- 定义要抽取哪些疾病
- 控制抽取边界
- 支撑任务拆分、进度管理

### 2. 疾病编码标准化

将自然语言诊断转换为标准ICD-10-CM编码，用于：
- 诊断匹配
- 知识库索引
- 临床术语标准化

### 3. 疾病分类检索

根据章节、节等信息，支持：
- 按系统分类检索疾病（如心血管系统、呼吸系统）
- 构建疾病分类树
- 支持层级浏览

## 注意事项

1. **文件大小**：XML文件较大（9-10MB），解析需要一些时间
2. **内存使用**：脚本使用iterparse流式解析，减少内存占用
3. **编码问题**：确保XML文件是UTF-8编码
4. **数据完整性**：索引和表格数据会合并，同一个编码在不同来源可能有不同描述

## 扩展建议

1. **添加中文名称**：可以手动或使用API添加中文疾病名称
2. **疾病分类映射**：根据章节映射到系统分类（心血管、呼吸等）
3. **常用疾病标记**：标记常见疾病，用于优先级排序
4. **同义词扩展**：从医疗词典扩展别名列表

## 问题反馈

如遇到问题，请检查：
1. XML文件路径是否正确
2. 文件是否完整下载
3. Python版本是否>=3.7
4. 是否有足够的磁盘空间
