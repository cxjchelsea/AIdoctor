# ICPC-2e-v7.0 XML文件解析和翻译使用说明

## 📋 概述

`parse_and_translate_icpc_xml.py` 脚本用于解析ICPC-2e-v7.0的XML文件并翻译为中文。XML文件包含比CSV文件更丰富的信息，包括：

- **preferred**: 主要术语（Title）
- **shortTitle**: 短标题
- **inclusion**: 包含内容（详细说明）
- **exclusion**: 排除内容
- **criteria**: 诊断标准
- **consider**: 考虑事项
- **note**: 注释
- **icd10**: ICD-10编码对照

## 🚀 快速开始

### 1. 配置AI翻译

确保 `ai_translation_config.json` 文件已正确配置：

```json
{
  "enabled": true,
  "api_url": "http://39.106.80.155:11434/api/generate",
  "api_type": "ollama",
  "api_key": null,
  "model": "qwen2.5:14b",
  "timeout": 30.0,
  "max_workers": 5
}
```

### 2. 运行翻译脚本

```bash
# 使用默认配置
python parse_and_translate_icpc_xml.py

# 指定输入输出文件
python parse_and_translate_icpc_xml.py \
  --input "源数据/ICPC-2e-v7.0/ICPC-2e-v7.0.xml" \
  --output-json "提取结果/icpc/icpc_xml_chinese.json" \
  --output-csv "提取结果/icpc/icpc_xml_chinese.csv"
```

## 📁 文件说明

### 输入文件

- **默认路径**: `docs/AI医生/3.知识内容提取/源数据/ICPC-2e-v7.0/ICPC-2e-v7.0.xml`
- **格式**: XML格式（ClaML标准格式）
- **内容**: 完整的ICPC分类信息，包括层级关系、详细描述等

### 输出文件

#### JSON格式（推荐）

- **默认路径**: `docs/AI医生/3.知识内容提取/提取结果/icpc/icpc_xml_chinese.json`
- **格式**: JSON数组，每个元素包含一个ICPC分类项的完整信息
- **字段说明**:
  - `code`: ICPC代码（如 "A01", "-30"）
  - `kind`: 类型（symptom, process, infection等）
  - `super_class`: 父类代码
  - `preferred`: 英文主要术语
  - `preferred_中文`: 中文主要术语
  - `shortTitle`: 英文短标题
  - `shortTitle_中文`: 中文短标题
  - `inclusion`: 英文包含内容
  - `inclusion_中文`: 中文包含内容
  - `exclusion`: 英文排除内容
  - `exclusion_中文`: 中文排除内容
  - `criteria`: 英文诊断标准
  - `criteria_中文`: 中文诊断标准
  - `consider`: 英文考虑事项
  - `consider_中文`: 中文考虑事项
  - `note`: 英文注释
  - `note_中文`: 中文注释
  - `icd10`: ICD-10编码对照

#### CSV格式

- **默认路径**: `docs/AI医生/3.知识内容提取/提取结果/icpc/icpc_xml_chinese.csv`
- **格式**: CSV表格，包含所有字段
- **用途**: 方便在Excel等工具中查看和编辑

### 缓存文件

- **路径**: `docs/AI医生/3.知识内容提取/提取结果/icpc/translation_cache.json`
- **作用**: 保存已翻译的术语，避免重复调用API
- **格式**: JSON格式，键为 `ai_英文术语`，值为中文翻译

## ⚙️ 功能特性

### 1. 完整信息提取

- 解析XML文件的所有Class元素
- 提取所有类型的Rubric（preferred, shortTitle, inclusion等）
- 保留层级关系（SuperClass, SubClass）

### 2. 智能翻译

- 自动识别需要翻译的字段
- 保留代码引用（如`<Reference>-34</Reference>`）
- 处理多行文本（inclusion, exclusion等可能包含多行）

### 3. 翻译缓存

- 已翻译的术语会保存到缓存文件中
- 下次运行时自动使用缓存
- 提高翻译速度，节省API调用

### 4. 并发翻译

- 支持多线程并发翻译
- 可通过 `max_workers` 配置并发数（建议3-10）
- 根据服务器性能调整

### 5. 错误处理

- API调用失败时自动处理
- 翻译失败的术语留空，不影响其他翻译
- 详细的错误日志输出

## 📊 使用示例

### 示例1：完整翻译

```bash
python parse_and_translate_icpc_xml.py
```

输出：
```
✓ 已从配置文件加载AI翻译设置
  API地址: http://39.106.80.155:11434/api/generate
  API类型: ollama
  模型: qwen2.5:14b
  超时: 30.0秒
  并发数: 5

正在解析XML文件: ...
✓ 解析完成，共 726 条记录

开始翻译...
  需要翻译: 2000+ 条
  已缓存: 0 条
  进度: 10/2000
  进度: 20/2000
  ...

✓ 翻译完成，共翻译 2000+ 个字段

正在保存翻译缓存...
✓ 缓存已保存

正在保存结果...
✓ JSON已保存: ...
✓ CSV已保存: ...

================================================================================
翻译统计
================================================================================
总记录数: 726
preferred: 726/726 (100.0%)
shortTitle: 726/726 (100.0%)
inclusion: 500/500 (100.0%)
exclusion: 300/300 (100.0%)
criteria: 200/200 (100.0%)
consider: 150/150 (100.0%)
note: 100/100 (100.0%)

示例数据（前3条）:
  -30: Medical examination/health evaluation complete -> 医学检查/健康评估完整
  A01: Pain general/multiple sites -> 全身/多处疼痛
  A02: Chills -> 寒战
```

### 示例2：只解析不翻译

如果AI翻译未启用，脚本会只解析XML文件，不进行翻译：

```bash
python parse_and_translate_icpc_xml.py
```

输出：
```
⚠️  AI翻译功能未启用，只解析不翻译

正在解析XML文件: ...
✓ 解析完成，共 726 条记录

正在保存结果...
✓ JSON已保存: ...
✓ CSV已保存: ...
```

## 📝 JSON输出示例

```json
[
  {
    "code": "A01",
    "kind": "symptom",
    "super_class": "A.1",
    "preferred": "Pain general/multiple sites",
    "preferred_中文": "全身/多处疼痛",
    "shortTitle": "Pain general/multiple sites",
    "shortTitle_中文": "全身/多处疼痛",
    "inclusion": "pain not just from one site/organ",
    "inclusion_中文": "非单一部位/器官的疼痛",
    "exclusion": "",
    "exclusion_中文": "",
    "criteria": "",
    "criteria_中文": "",
    "consider": "",
    "consider_中文": "",
    "note": "",
    "note_中文": "",
    "icd10": "R52.0; R52.1; R52.9"
  },
  {
    "code": "-30",
    "kind": "process",
    "super_class": "-.2",
    "preferred": "Medical examination/health evaluation complete",
    "preferred_中文": "医学检查/健康评估完整",
    "shortTitle": "Medical examin/health eval complete",
    "shortTitle_中文": "医学检查/健康评估完整",
    "inclusion": "complete examination of one body system or the whole body; complete check-up; well-baby exam; school health care exam for children/youth",
    "inclusion_中文": "一个身体系统或全身的完整检查；完整体检；婴儿健康检查；儿童/青少年学校健康检查",
    "exclusion": "",
    "exclusion_中文": "",
    "criteria": "",
    "criteria_中文": "",
    "consider": "",
    "consider_中文": "",
    "note": "",
    "note_中文": "",
    "icd10": ""
  }
]
```

## 🔧 常见问题

### Q1: XML文件解析失败怎么办？

**可能原因**：
- XML文件格式不正确
- 文件编码问题
- XML文件损坏

**解决方案**：
1. 检查XML文件是否完整
2. 确认文件编码为UTF-8
3. 尝试用XML编辑器打开文件验证格式

### Q2: 翻译速度慢怎么办？

1. **增加并发数**：修改 `max_workers`（建议3-10）
2. **使用缓存**：已翻译的术语会使用缓存，速度很快
3. **检查网络**：确保API服务器响应速度正常
4. **分批翻译**：可以先翻译部分字段（修改代码）

### Q3: 翻译质量不理想怎么办？

1. **调整提示词**：修改 `_build_translation_prompt` 方法中的提示词
2. **更换模型**：使用更专业的医学翻译模型
3. **人工审核**：翻译完成后进行人工审核和修正
4. **建立术语库**：对常用术语建立标准翻译字典

### Q4: 如何只翻译部分字段？

可以修改脚本中的 `translatable_rubrics` 集合，只包含需要翻译的字段类型。

### Q5: 如何处理Reference标签？

脚本会自动保留XML中的`<Reference>`标签，翻译时不会翻译标签内的代码。

### Q6: 翻译缓存文件损坏怎么办？

删除 `translation_cache.json` 文件，重新运行脚本即可。

## 📝 注意事项

1. **首次使用**：建议先用少量数据测试，确认翻译质量和API连接正常
2. **翻译缓存**：已翻译的内容会保存，避免重复调用API
3. **超时设置**：根据服务器响应速度调整 `timeout`
4. **API限制**：注意API调用频率限制，避免过载
5. **医学术语**：AI翻译可能不完全准确，建议进行人工审核
6. **数据量**：XML文件包含大量文本，翻译可能需要较长时间

## 🎯 下一步

翻译完成后，可以：

1. **审核翻译质量**：检查翻译结果，修正不准确的翻译
2. **构建知识图谱**：利用层级关系（SuperClass, SubClass）构建知识图谱
3. **集成到系统**：将翻译结果集成到AI医生系统中
4. **建立术语库**：提取常用术语，建立标准翻译字典

## 📚 相关文档

- [ICPC CSV翻译脚本](./README_ICPC翻译.md)
- [通用数据集中文翻译方案.md](../提取方案/通用数据集中文翻译方案.md)
- [README_AI翻译配置.md](./README_AI翻译配置.md)

## 🔍 XML文件结构说明

ICPC XML文件使用ClaML标准格式，主要结构：

```xml
<ClaML>
  <Class code="A01" kind="symptom">
    <SuperClass code="A.1"/>
    <Rubric kind="preferred">
      <Label xml:lang="en">Pain general/multiple sites</Label>
    </Rubric>
    <Rubric kind="shortTitle">
      <Label xml:lang="en">Pain general/multiple sites</Label>
    </Rubric>
    <Rubric kind="inclusion">
      <Label xml:lang="en">pain not just from one site/organ</Label>
    </Rubric>
    <Rubric kind="icd10">
      <Label xml:lang="en">R52.0; R52.1; R52.9</Label>
    </Rubric>
  </Class>
</ClaML>
```

脚本会解析所有这些信息并翻译需要翻译的部分。

