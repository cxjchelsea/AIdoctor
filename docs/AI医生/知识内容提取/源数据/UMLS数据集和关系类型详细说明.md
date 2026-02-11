# UMLS数据集和关系类型详细说明

## 一、UMLS简介

### 1.1 什么是UMLS？

**UMLS（Unified Medical Language System，统一医学语言系统）** 是由美国国家医学图书馆（NLM）开发和维护的一个大型生物医学知识库。它是目前世界上最大、最全面的医学知识图谱之一。

### 1.2 UMLS的核心组成

UMLS主要由三个部分组成：

1. **Metathesaurus（元词典）**
   - 整合了来自200多个生物医学术语源的概念
   - 包含超过450万个医学概念（CUI - Concept Unique Identifier）
   - 每个概念都有唯一的CUI标识符
   - 包含1500多万个概念间的关系

2. **Semantic Network（语义网络）**
   - 定义了133种语义类型（如"疾病"、"症状"、"药物"等）
   - 定义了54种语义关系（如"导致"、"治疗"、"诊断"等）

3. **SPECIALIST Lexicon（专业词典）**
   - 包含医学术语的词汇信息
   - 支持自然语言处理任务

### 1.3 UMLS在DR.Knows项目中的应用

在DR.Knows项目中，UMLS被用作：
- **知识图谱数据源**：构建包含医学概念和关系的知识图谱
- **概念标准化**：将临床文本中的医学概念映射到标准化的CUI
- **推理路径生成**：通过概念间的关系构建诊断推理路径

## 二、UMLS_refined_relations_DRKnows.csv文件说明

### 2.1 文件来源和背景

**重要说明**：DR.Knows项目**并未使用UMLS中的所有关系类型**。

- 一位经过认证的医师科学家（board-certified physician scientist）手动检查了UMLS中的所有关系
- 从中筛选出**与诊断推理相关**的108种关系类型
- **排除了概念层次结构关系**（如父子关系、is-a关系等，这些主要用于分类而非推理）
- 这108种关系专门用于支持**诊断推理**任务

### 2.2 文件结构

CSV文件包含以下列：

| 列名 | 说明 | 示例 |
|------|------|------|
| **Relation** | 关系类型名称（英文） | `has finding site`, `causative agent of`, `due to` |
| **Count** | 该关系在UMLS中出现的总次数 | `232780.0` 表示该关系出现了232,780次 |
| **Example 1** | 第一个关系示例 | `Childhood cerebral X-linked adrenoleukodystrophy (disorder) -- finding site of-> Adrenal cortex structure` |
| **Example 2** | 第二个关系示例 | `Closed bimalleolar fracture (disorder) -- finding site of-> Foot structure` |
| **Example 3** | 第三个关系示例 | `Malignant neoplasm of other sites of lip -- finding site of-> Ill-defined topographic site` |

### 2.3 关系示例格式

每个示例的格式为：
```
源概念 (类型) -- 关系类型-> 目标概念 (类型)
```

例如：
- `Childhood cerebral X-linked adrenoleukodystrophy (disorder) -- finding site of-> Adrenal cortex structure (body structure)`
  - 含义：儿童脑部X连锁肾上腺脑白质营养不良（疾病）的发现部位是肾上腺皮质结构（身体结构）

## 三、关系类型分类

根据关系在诊断推理中的作用，可以将这108种关系大致分为以下几类：

### 3.1 解剖结构相关关系（Anatomy Relations）

**作用**：描述疾病、症状、检查与身体部位的关系

| 关系类型 | 数量 | 说明 | 示例 |
|---------|------|------|------|
| `finding site of` | 232,780 | 疾病的发现部位 | 骨折 → 足部结构 |
| `has finding site` | 232,785 | 身体结构上发现的疾病 | 足部结构 → 骨折 |
| `has procedure site` | 75,767 | 手术/检查的操作部位 | 血管 → 血管结扎术 |
| `procedure site of` | 75,767 | 手术/检查针对的身体部位 | 血管结扎术 → 血管 |
| `has direct procedure site` | 15,510 | 手术的直接操作部位 | 血管结构 → CT血管造影 |
| `direct procedure site of` | 15,508 | 手术直接针对的部位 | CT血管造影 → 血管结构 |

### 3.2 因果关系（Causality Relations）

**作用**：描述疾病、症状之间的因果关系

| 关系类型 | 数量 | 说明 | 示例 |
|---------|------|------|------|
| `causative agent of` | 56,497 | 致病因子 | 药物中毒 → 药物物质 |
| `cause of` | 18,439 | 导致某疾病的原因 | 创伤事件 → 肘关节脱位 |
| `due to` | 17,976 | 由...引起 | 创伤事件 → 闭合性肘关节脱位 |
| `has pathological process` | 35,415 | 具有的病理过程 | 肿瘤过程 → 良性肿瘤 |
| `pathological process of` | 35,412 | 疾病的病理过程 | 病毒性肺炎 → 传染病 |

### 3.3 时间关系（Temporal Relations）

**作用**：描述疾病、事件的时间顺序

| 关系类型 | 数量 | 说明 | 示例 |
|---------|------|------|------|
| `occurs before` | 5,629 | 发生在...之前 | 过敏反应 → 过敏致敏 |
| `occurs after` | 5,630 | 发生在...之后 | 损伤 → 创伤性白内障 |
| `temporally follows` | 2,978 | 时间上跟随 | 手术 → 组织扩张器暴露 |
| `temporally followed by` | 2,979 | 时间上被跟随 | 部分失败再植趾 → 植入 |
| `during` | 242 | 在...期间 | 透析程序 → 非过敏性超敏反应 |

### 3.4 临床表现关系（Clinical Manifestation Relations）

**作用**：描述疾病的症状、体征表现

| 关系类型 | 数量 | 说明 | 示例 |
|---------|------|------|------|
| `has definitional manifestation` | 9,675 | 具有的定义性表现 | 血小板减少症 → 其他原发性血小板减少症 |
| `definitional manifestation of` | 9,713 | 是...的定义性表现 | 过度VIP分泌 → 腹泻 |
| `has associated finding` | 8,712 | 具有的关联发现 | 患者报告的发现 → 口臭 |
| `associated finding of` | 8,670 | 是...的关联发现 | 疑似副伤寒 → 副伤寒 |
| `associated with` | 5,836 | 与...相关 | 裸金属支架 → 左冠状动脉前降支裸金属支架 |

### 3.5 形态学关系（Morphology Relations）

**作用**：描述疾病的形态学特征

| 关系类型 | 数量 | 说明 | 示例 |
|---------|------|------|------|
| `has associated morphology` | 131,887 | 具有的关联形态 | 炎症形态 → 血清阴性类风湿关节炎 |
| `associated morphology of` | 134,899 | 是...的关联形态 | 牵引性脱发 → 组织损伤 |
| `has direct morphology` | 11,572 | 具有的直接形态 | 囊状扩张 → 输尿管囊肿切除术 |
| `has indirect morphology` | 854 | 具有的间接形态 | 溃疡 → 足部溃疡清创术 |

### 3.6 药物和物质关系（Drug and Substance Relations）

**作用**：描述药物、物质的成分和作用

| 关系类型 | 数量 | 说明 | 示例 |
|---------|------|------|------|
| `active ingredient of` | 43,169 | 是...的活性成分 | 依普利酮片 → 降压药 |
| `has precise active ingredient` | 5,932 | 具有的精确活性成分 | 阿米替林产品 → 阿米替林片 |
| `disposition of` | 10,066 | 是...的处置方式 | 匹伐他汀钙 → HMG-CoA还原酶抑制剂 |
| `has disposition` | 10,041 | 具有的处置方式 | 胆碱酯酶抑制剂 → 加兰他敏 |
| `dose form of` | 28,211 | 是...的剂型 | 直肠阿司匹林 → 直肠剂型 |

### 3.7 程序和方法关系（Procedure and Method Relations）

**作用**：描述医疗程序、检查方法

| 关系类型 | 数量 | 说明 | 示例 |
|---------|------|------|------|
| `method of` | 125,899 | 是...的方法 | 脊髓减压术 → 减压手术 |
| `has method` | 123,874 | 具有的方法 | 手术操作 → 外直肌切开术 |
| `technique of` | 464 | 是...的技术 | 24小时半乳糖摄入量测量 → 测量 |
| `approach of` | 1,988 | 是...的入路 | 玻璃体替代物注射 → 睫状体平坦部入路 |

### 3.8 解释和诊断关系（Interpretation Relations）

**作用**：描述检查结果与诊断的关系

| 关系类型 | 数量 | 说明 | 示例 |
|---------|------|------|------|
| `interprets` | 77,467 | 解释/诊断 | 氧输送 → 紫绀型先天性心脏病 |
| `is interpreted by` | 77,715 | 被...解释 | 脊柱骨骺发育不良 → 身高和生长 |

### 3.9 其他重要关系

| 关系类型 | 数量 | 说明 |
|---------|------|------|
| `isa` | 924,325 | 是...的一种（虽然排除了层次结构，但isa关系在诊断中仍有重要作用） |
| `same as` | 41,406 | 等同于（同义词关系） |
| `has part` / `part of` | 52,405 / 52,359 | 部分关系 |
| `has component` / `component of` | 9,752 / 9,755 | 组成关系 |
| `has occurrence` | 18,746 | 发生情况（如先天性、获得性） |
| `has severity` | 505 | 严重程度 |
| `has clinical course` | 4,373 | 临床病程（急性、慢性） |

## 四、如何使用这个文件

### 4.1 在DR.Knows项目中的使用

1. **构建知识图谱**
   - 从UMLS中提取只包含这108种关系的子图
   - 过滤掉不相关的层次结构关系
   - 构建专门用于诊断推理的知识图谱

2. **路径检索**
   - 在知识图谱中检索多跳推理路径时，只使用这108种关系类型
   - 确保检索到的路径与诊断推理相关

3. **关系类型过滤**
   - 在代码中可以使用这个CSV文件作为白名单
   - 只保留这108种关系类型，过滤其他关系

### 4.2 代码示例

```python
import pandas as pd

# 读取关系类型文件
relations_df = pd.read_csv('UMLS_refined_relations_DRKnows.csv')

# 获取所有关系类型名称
valid_relations = set(relations_df['Relation'].tolist())

# 在构建知识图谱时过滤关系
def filter_relations(edge_relation):
    """只保留与诊断推理相关的关系"""
    return edge_relation in valid_relations

# 使用示例
# 假设从UMLS中提取的边包含关系类型
for source, target, relation in graph_edges:
    if filter_relations(relation):
        # 添加到知识图谱
        knowledge_graph.add_edge(source, target, relation=relation)
```

### 4.3 关系类型统计和分析

```python
import pandas as pd
import matplotlib.pyplot as plt

# 读取数据
df = pd.read_csv('UMLS_refined_relations_DRKnows.csv')

# 查看关系数量分布
print(f"总关系类型数: {len(df)}")
print(f"总关系实例数: {df['Count'].sum():,.0f}")

# 查看最常见的关系类型（Top 10）
top_relations = df.nlargest(10, 'Count')
print("\n最常见的10种关系类型:")
print(top_relations[['Relation', 'Count']])

# 按关系类别分组统计（需要根据实际分类）
# 这里只是示例，实际使用时需要根据关系语义进行分类
```

### 4.4 关系类型查询

```python
# 查找特定类型的关系
def find_relations_by_keyword(keyword):
    """根据关键词查找相关的关系类型"""
    matching = df[df['Relation'].str.contains(keyword, case=False)]
    return matching

# 示例：查找所有与"cause"相关的关系
cause_relations = find_relations_by_keyword('cause')
print(cause_relations[['Relation', 'Count']])
```

## 五、关系类型详细列表

以下是108种关系类型的完整列表（按Count降序排列的前20种）：

| 序号 | 关系类型 | 数量 | 主要用途 |
|------|---------|------|----------|
| 1 | `isa` | 924,325 | 概念分类关系（虽然排除了层次结构，但isa在诊断中仍有重要作用） |
| 2 | `has finding site` | 232,785 | 身体结构上发现的疾病 |
| 3 | `finding site of` | 232,780 | 疾病的发现部位 |
| 4 | `has method` | 123,874 | 程序/检查的方法 |
| 5 | `method of` | 125,899 | 是...的方法 |
| 6 | `has procedure site` | 75,767 | 手术/检查的操作部位 |
| 7 | `procedure site of` | 75,767 | 手术/检查针对的身体部位 |
| 8 | `interprets` | 77,467 | 解释/诊断关系 |
| 9 | `is interpreted by` | 77,715 | 被...解释 |
| 10 | `associated morphology of` | 134,899 | 疾病的形态学特征 |
| 11 | `has associated morphology` | 131,887 | 具有的形态学特征 |
| 12 | `causative agent of` | 56,497 | 致病因子 |
| 13 | `same as` | 41,406 | 同义词关系 |
| 14 | `active ingredient of` | 43,169 | 药物活性成分 |
| 15 | `has pathological process` | 35,415 | 病理过程 |
| 16 | `pathological process of` | 35,412 | 疾病的病理过程 |
| 17 | `has intent` | 23,422 | 意图（诊断、治疗等） |
| 18 | `intent of` | 23,420 | 是...的意图 |
| 19 | `has occurrence` | 18,746 | 发生情况 |
| 20 | `cause of` | 18,439 | 导致...的原因 |

## 六、注意事项

### 6.1 关系方向性

- 大多数关系是**有方向的**（directed）
- 例如：`finding site of` 和 `has finding site` 是相反方向的关系
- 在构建知识图谱时需要注意关系的方向

### 6.2 关系数量差异

- 不同关系的实例数量差异很大
- `isa`关系有92万多个实例，而某些关系只有几十个实例
- 在使用时需要考虑关系的频率和重要性

### 6.3 关系语义

- 每种关系都有特定的医学语义
- 在诊断推理中，不同关系的重要性不同
- 例如：因果关系（`cause of`, `due to`）在诊断推理中可能比形态学关系更重要

### 6.4 与完整UMLS的关系

- 这108种关系是UMLS所有关系的**子集**
- 专门针对**诊断推理**任务筛选
- 如果用于其他任务（如药物发现、文献检索），可能需要不同的关系集合

## 七、参考资料

1. **UMLS官方文档**：https://www.nlm.nih.gov/research/umls/
2. **DR.Knows论文**：
   - Gao, Y., et al. (2025). Leveraging Medical Knowledge Graphs Into Large Language Models for Diagnosis Prediction: Design and Application Study. JMIR AI, 4, e58670.
3. **SNOMED CT**：UMLS的主要术语源之一，包含大量临床概念和关系

## 八、总结

`UMLS_refined_relations_DRKnows.csv`文件是DR.Knows项目中的核心资源，它：

1. **定义了诊断推理相关的108种关系类型**
2. **提供了每种关系的统计信息和示例**
3. **用于构建专门用于诊断推理的知识图谱**
4. **确保知识图谱中的关系都与诊断推理相关**

在使用UMLS构建医学知识图谱时，应该：
- 使用这个文件作为关系类型的白名单
- 只保留这108种关系类型
- 根据关系的医学语义和频率进行适当的权重设置
- 在路径检索和推理时考虑关系的方向性和语义

---

**文档版本**：v1.0  
**最后更新**：2025年  
**维护者**：DR.Knows项目组