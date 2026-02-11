# HPO 数据结构定义与提取映射文档

本文档定义了HPO数据的目标数据结构，以及每个字段从OWL源数据中的提取位置和方法。

---

## 一、目标数据结构定义

### 1.1 完整数据结构（JSON格式）

```json
{
  "metadata": {
    // 元数据：版本号、数据源、提取日期、统计信息
    "version": "2026-01-08",           // 版本号：HPO本体版本
    "source": "hp.owl",                 // 数据源：源文件名称
    "extraction_date": "2025-01-XX",    // 提取日期：数据提取时间
    "total_terms": 15000,                // 术语总数：提取的HPO术语数量
    "total_synonyms": 50000              // 同义词总数：所有同义词的数量
  },
  "terms": {
    "HP:0000003": {
      // 术语基本信息
      "id": "HP:0000003",                // 术语ID：唯一标识符
      "label": "Multicystic kidney dysplasia",  // 标准名称：术语的标准英文名称
      "definition": "Multicystic dysplasia of the kidney...",  // 定义：术语的正式定义说明
      "comment": "Multicystic kidney dysplasia is the result...",  // 注释：术语的补充说明信息
      
      // 同义词（核心，用于症状归一化）
      "exact_synonyms": [                // 精确同义词：与标准名称完全等价的同义词
        "Multicystic dysplastic kidney",
        "Multicystic kidneys",
        "Multicystic renal dysplasia"
      ],
      "related_synonyms": [],            // 相关同义词：与标准名称相关但不完全等价的同义词
      
      // 层级关系（用于分类）
      "parent_ids": ["HP:0000107"],      // 父类ID列表：该术语的上级分类术语ID列表
      "child_ids": [],                   // 子类ID列表：该术语的下级分类术语ID列表
      "is_leaf": true,                   // 是否为叶子节点：没有子类的术语，通常是具体的症状术语
      
      // 版本管理
      "alternative_ids": ["HP:0004715"], // 替代ID列表：该术语的历史ID或替代ID，用于版本兼容
      "deprecated": false,               // 是否废弃：标记该术语是否已被废弃
      "replaced_by": null,               // 被替代的术语ID：如果该术语已被替代，指向新的术语ID
      "creation_date": "2008-02-27T02:20:00Z",  // 创建日期：该术语在HPO中的创建时间
      
      // 外部引用
      "xrefs": [                         // 外部引用列表：该术语在其他医学数据库中的对应ID
        {"type": "UMLS", "id": "C3714581"},
        {"type": "SNOMEDCT_US", "id": "204962002"},
        {"type": "SNOMEDCT_US", "id": "82525005"}
      ]
    }
  },
  // 快速查找索引
  "synonym_index": {                     // 同义词索引：用于快速查找，通过同义词文本查找对应的HPO术语ID
    "Multicystic dysplastic kidney": "HP:0000003",
    "Multicystic kidneys": "HP:0000003",
    "Multicystic renal dysplasia": "HP:0000003"
  },
  "alternative_id_mapping": {            // 替代ID映射：用于版本兼容，通过旧ID或替代ID查找新的标准ID
    "HP:0004715": "HP:0000003"
  },
  "deprecated_terms": []                 // 废弃术语列表：所有已被废弃的术语ID列表，用于过滤和避免使用
}
```

---

## 二、字段提取映射表

### 2.1 metadata（元数据）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `version` | 版本号 | string | `<owl:versionIRI>` 属性 | 从 `<owl:Ontology>` 标签的 `owl:versionIRI` 属性中提取日期部分 | `"2026-01-08"` |
| `source` | 数据源 | string | 固定值 | 固定为 `"hp.owl"` | `"hp.owl"` |
| `extraction_date` | 提取日期 | string | 系统生成 | 提取时的当前日期 | `"2025-01-15"` |
| `total_terms` | 术语总数 | integer | 计算得出 | 统计 `terms` 对象的数量 | `15000` |
| `total_synonyms` | 同义词总数 | integer | 计算得出 | 统计 `synonym_index` 对象的数量 | `50000` |

**OWL源数据位置**：
```xml
<owl:Ontology rdf:about="http://purl.obolibrary.org/obo/hp.owl">
    <owl:versionIRI rdf:resource="http://purl.obolibrary.org/obo/hp/releases/2026-01-08/hp.owl"/>
    ...
</owl:Ontology>
```

---

### 2.2 terms[].id（术语ID）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `id` | 术语ID（唯一标识符） | string | `<oboInOwl:id>` 元素 | 从 `<owl:Class>` 标签内的 `<oboInOwl:id>` 元素中提取 | `"HP:0000003"` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000003">
    <oboInOwl:id>HP:0000003</oboInOwl:id>
    ...
</owl:Class>
```

**提取逻辑**：
- 查找所有 `<owl:Class rdf:about="...HP_...">` 标签
- 提取其中的 `<oboInOwl:id>` 元素内容
- 只保留以 `HP:` 开头的ID

---

### 2.3 terms[].label（标准名称）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `label` | 标准名称（术语的标准英文名称） | string | `<rdfs:label>` 元素 | 从 `<owl:Class>` 标签内的 `<rdfs:label>` 元素中提取，优先提取 `xml:lang="en"` 的 | `"Multicystic kidney dysplasia"` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000003">
    <rdfs:label>Multicystic kidney dysplasia</rdfs:label>
    <!-- 或 -->
    <rdfs:label xml:lang="en">Multicystic kidney dysplasia</rdfs:label>
    ...
</owl:Class>
```

**提取逻辑**：
- 查找 `<rdfs:label>` 元素
- 如果有 `xml:lang="en"`，优先使用
- 如果没有，使用第一个 `<rdfs:label>` 元素

---

### 2.4 terms[].definition（定义）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `definition` | 定义（术语的正式定义说明） | string | `<obo:IAO_0000115>` 元素 | 从 `<owl:Class>` 标签内的 `<obo:IAO_0000115>` 元素中提取 | `"Multicystic dysplasia of the kidney..."` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000003">
    <obo:IAO_0000115>Multicystic dysplasia of the kidney is characterized by multiple cysts of varying size in the kidney and the absence of a normal pelvicaliceal system. The condition is associated with ureteral or ureteropelvic atresia, and the affected kidney is nonfunctional.</obo:IAO_0000115>
    ...
</owl:Class>
```

**提取逻辑**：
- 查找 `<obo:IAO_0000115>` 元素
- 提取其文本内容
- 注意：某些定义可能在 `<owl:Axiom>` 中（带注解的定义），需要关联提取

**带注解的定义（在owl:Axiom中）**：
```xml
<owl:Axiom>
    <owl:annotatedSource rdf:resource="http://purl.obolibrary.org/obo/HP_0000003"/>
    <owl:annotatedProperty rdf:resource="http://purl.obolibrary.org/obo/IAO_0000115"/>
    <owl:annotatedTarget>Multicystic dysplasia of the kidney...</owl:annotatedTarget>
</owl:Axiom>
```
- 需要关联 `annotatedSource` 和对应的 `owl:Class`

---

### 2.5 terms[].comment（注释）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `comment` | 注释（术语的补充说明信息） | string | `<rdfs:comment>` 元素 | 从 `<owl:Class>` 标签内的 `<rdfs:comment>` 元素中提取 | `"Multicystic kidney dysplasia is the result..."` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000003">
    <rdfs:comment>Multicystic kidney dysplasia is the result of abnormal fetal renal development in which the affected kidney is replaced by multiple cysts and has little or no residual function. The vast majority of multicystic kidneys are unilateral. Multicystic kidney can be diagnosed on prenatal ultrasound.</rdfs:comment>
    ...
</owl:Class>
```

**提取逻辑**：
- 查找 `<rdfs:comment>` 元素
- 提取其文本内容
- 注意：某些术语可能没有comment，可以为空

---

### 2.6 terms[].exact_synonyms（精确同义词）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `exact_synonyms` | 精确同义词（与标准名称完全等价的同义词，用于症状归一化） | array[string] | `<oboInOwl:hasExactSynonym>` 元素 | 从 `<owl:Class>` 标签内的所有 `<oboInOwl:hasExactSynonym>` 元素中提取 | `["Multicystic dysplastic kidney", "Multicystic kidneys", "Multicystic renal dysplasia"]` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000003">
    <oboInOwl:hasExactSynonym>Multicystic dysplastic kidney</oboInOwl:hasExactSynonym>
    <oboInOwl:hasExactSynonym>Multicystic kidneys</oboInOwl:hasExactSynonym>
    <oboInOwl:hasExactSynonym>Multicystic renal dysplasia</oboInOwl:hasExactSynonym>
    ...
</owl:Class>
```

**带注解的同义词（在owl:Axiom中）**：
```xml
<owl:Axiom>
    <owl:annotatedSource rdf:resource="http://purl.obolibrary.org/obo/HP_0000003"/>
    <owl:annotatedProperty rdf:resource="http://www.geneontology.org/formats/oboInOwl#hasExactSynonym"/>
    <owl:annotatedTarget>Multicystic dysplastic kidney</owl:annotatedTarget>
    <oboInOwl:hasSynonymType rdf:resource="http://purl.obolibrary.org/obo/hp#layperson"/>
</owl:Axiom>
```

**提取逻辑**：
- 查找所有 `<oboInOwl:hasExactSynonym>` 元素（在 `<owl:Class>` 内）
- 查找所有 `<owl:Axiom>` 中 `annotatedProperty` 为 `hasExactSynonym` 的，提取 `annotatedTarget`
- 合并去重
- 提取所有文本内容，组成数组

---

### 2.7 terms[].related_synonyms（相关同义词）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `related_synonyms` | 相关同义词（与标准名称相关但不完全等价的同义词） | array[string] | `<oboInOwl:hasRelatedSynonym>` 元素 | 从 `<owl:Class>` 标签内的所有 `<oboInOwl:hasRelatedSynonym>` 元素中提取 | `["Autosomal dominant form", "Autosomal dominant type"]` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000006">
    <oboInOwl:hasRelatedSynonym>Autosomal dominant form</oboInOwl:hasRelatedSynonym>
    <oboInOwl:hasRelatedSynonym>Autosomal dominant type</oboInOwl:hasRelatedSynonym>
    ...
</owl:Class>
```

**带注解的相关同义词（在owl:Axiom中）**：
```xml
<owl:Axiom>
    <owl:annotatedSource rdf:resource="http://purl.obolibrary.org/obo/HP_0000006"/>
    <owl:annotatedProperty rdf:resource="http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym"/>
    <owl:annotatedTarget>Autosomal dominant form</owl:annotatedTarget>
    <oboInOwl:hasDbXref rdf:resource="https://orcid.org/0000-0002-5316-1399"/>
</owl:Axiom>
```

**提取逻辑**：
- 查找所有 `<oboInOwl:hasRelatedSynonym>` 元素（在 `<owl:Class>` 内）
- 查找所有 `<owl:Axiom>` 中 `annotatedProperty` 为 `hasRelatedSynonym` 的，提取 `annotatedTarget`
- 合并去重
- 提取所有文本内容，组成数组

---

### 2.8 terms[].parent_ids（父类ID列表）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `parent_ids` | 父类ID列表（该术语的上级分类术语ID列表，用于构建层级结构） | array[string] | `<rdfs:subClassOf>` 属性 | 从 `<owl:Class>` 标签的 `<rdfs:subClassOf>` 属性中提取，将URI转换为HPO ID | `["HP:0000107"]` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000003">
    <rdfs:subClassOf rdf:resource="http://purl.obolibrary.org/obo/HP_0000107"/>
    ...
</owl:Class>
```

**复杂情况（Restriction）**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000002">
    <rdfs:subClassOf rdf:resource="http://purl.obolibrary.org/obo/HP_0001507"/>
    <owl:equivalentClass>
        <owl:Restriction>
            <owl:onProperty rdf:resource="..."/>
            ...
        </owl:Restriction>
    </owl:equivalentClass>
</owl:Class>
```

**提取逻辑**：
- 查找所有 `<rdfs:subClassOf rdf:resource="...">` 属性
- 提取 `rdf:resource` 的值（URI）
- 从URI中提取HPO ID（例如：`http://purl.obolibrary.org/obo/HP_0000107` → `HP:0000107`）
- 注意：如果 `rdfs:subClassOf` 指向的是 `<owl:Restriction>` 而不是直接的类，需要解析Restriction结构，提取其中的类引用
- 组成数组

---

### 2.9 terms[].child_ids（子类ID列表）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `child_ids` | 子类ID列表（该术语的下级分类术语ID列表，用于构建层级结构） | array[string] | 计算得出 | 通过遍历所有术语，找出 `parent_ids` 中包含当前术语ID的术语 | `[]` |

**提取逻辑**：
- 不是直接从OWL源数据提取
- 遍历所有术语，对于每个术语：
  - 如果其 `parent_ids` 中包含当前术语的ID，则将当前术语ID添加到该术语的 `child_ids` 中
- 例如：如果 `HP:0000003` 的 `parent_ids` 是 `["HP:0000107"]`，则将 `HP:0000003` 添加到 `HP:0000107` 的 `child_ids` 中

---

### 2.10 terms[].is_leaf（是否为叶子节点）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `is_leaf` | 是否为叶子节点（没有子类的术语，通常是具体的症状术语） | boolean | 计算得出 | 如果 `child_ids` 为空数组，则为 `true`，否则为 `false` | `true` |

**提取逻辑**：
- 不是直接从OWL源数据提取
- 计算：`is_leaf = (len(child_ids) == 0)`
- 叶子节点表示具体的症状术语（没有子类）

---

### 2.11 terms[].alternative_ids（替代ID列表）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `alternative_ids` | 替代ID列表（该术语的历史ID或替代ID，用于版本兼容） | array[string] | `<oboInOwl:hasAlternativeId>` 元素 | 从 `<owl:Class>` 标签内的所有 `<oboInOwl:hasAlternativeId>` 元素中提取 | `["HP:0004715"]` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000003">
    <oboInOwl:hasAlternativeId>HP:0004715</oboInOwl:hasAlternativeId>
    ...
</owl:Class>
```

**多个替代ID**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000005">
    <oboInOwl:hasAlternativeId>HP:0001425</oboInOwl:hasAlternativeId>
    <oboInOwl:hasAlternativeId>HP:0001453</oboInOwl:hasAlternativeId>
    <oboInOwl:hasAlternativeId>HP:0001461</oboInOwl:hasAlternativeId>
    <oboInOwl:hasAlternativeId>HP:0010985</oboInOwl:hasAlternativeId>
    ...
</owl:Class>
```

**提取逻辑**：
- 查找所有 `<oboInOwl:hasAlternativeId>` 元素
- 提取所有文本内容，组成数组

---

### 2.12 terms[].deprecated（是否废弃）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `deprecated` | 是否废弃（标记该术语是否已被废弃，废弃的术语不应使用） | boolean | `<owl:deprecated>` 元素 | 从 `<owl:Class>` 标签内的 `<owl:deprecated>` 元素中提取，如果存在且值为 `true`，则为 `true`，否则为 `false` | `false` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_XXXXXX">
    <owl:deprecated rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:deprecated>
    ...
</owl:Class>
```

**提取逻辑**：
- 查找 `<owl:deprecated>` 元素
- 如果存在且值为 `"true"` 或 `true`，则返回 `true`
- 如果不存在或值为 `"false"`，则返回 `false`

---

### 2.13 terms[].replaced_by（被替代的术语ID）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `replaced_by` | 被替代的术语ID（如果该术语已被替代，指向新的术语ID） | string\|null | `<obo:IAO_0100001>` 属性 | 从 `<owl:Class>` 标签的 `<obo:IAO_0100001>` 属性中提取，将URI转换为HPO ID | `null` 或 `"HP:XXXXXX"` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_OLD_ID">
    <obo:IAO_0100001 rdf:resource="http://purl.obolibrary.org/obo/HP_NEW_ID"/>
    ...
</owl:Class>
```

**提取逻辑**：
- 查找 `<obo:IAO_0100001 rdf:resource="...">` 属性
- 提取 `rdf:resource` 的值（URI）
- 从URI中提取HPO ID
- 如果不存在，返回 `null`

---

### 2.14 terms[].creation_date（创建日期）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `creation_date` | 创建日期（该术语在HPO中的创建时间，用于版本管理） | string\|null | `<oboInOwl:creation_date>` 元素 | 从 `<owl:Class>` 标签内的 `<oboInOwl:creation_date>` 元素中提取 | `"2008-02-27T02:20:00Z"` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000002">
    <oboInOwl:creation_date>2008-02-27T02:20:00Z</oboInOwl:creation_date>
    ...
</owl:Class>
```

**带数据类型的日期**：
```xml
<oboInOwl:creation_date rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime">2018-03-13T23:55:05Z</oboInOwl:creation_date>
```

**提取逻辑**：
- 查找 `<oboInOwl:creation_date>` 元素
- 提取其文本内容（ISO 8601格式）
- 如果不存在，返回 `null`

---

### 2.15 terms[].xrefs（外部引用列表）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `xrefs` | 外部引用列表（该术语在其他医学数据库中的对应ID，如UMLS、SNOMED CT等） | array[object] | `<oboInOwl:hasDbXref>` 元素 | 从 `<owl:Class>` 标签内的所有 `<oboInOwl:hasDbXref>` 元素中提取，解析类型和ID | `[{"type": "UMLS", "id": "C3714581"}, {"type": "SNOMEDCT_US", "id": "204962002"}]` |

**OWL源数据位置**：
```xml
<owl:Class rdf:about="http://purl.obolibrary.org/obo/HP_0000003">
    <oboInOwl:hasDbXref>SNOMEDCT_US:204962002</oboInOwl:hasDbXref>
    <oboInOwl:hasDbXref>SNOMEDCT_US:82525005</oboInOwl:hasDbXref>
    <oboInOwl:hasDbXref>UMLS:C3714581</oboInOwl:hasDbXref>
    ...
</owl:Class>
```

**提取逻辑**：
- 查找所有 `<oboInOwl:hasDbXref>` 元素
- 解析每个xref的格式：`TYPE:ID`（例如：`UMLS:C3714581`）
- 分割为 `type` 和 `id`
- 组成对象数组：`[{"type": "UMLS", "id": "C3714581"}, ...]`

---

### 2.16 synonym_index（同义词索引）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `synonym_index` | 同义词索引（用于快速查找：通过同义词文本查找对应的HPO术语ID，这是症状归一化的核心索引） | object | 计算得出 | 遍历所有术语，将每个术语的 `exact_synonyms` 和 `related_synonyms` 作为key，术语ID作为value | `{"Multicystic dysplastic kidney": "HP:0000003", ...}` |

**提取逻辑**：
- 不是直接从OWL源数据提取
- 遍历所有术语：
  - 对于每个 `exact_synonym` 和 `related_synonym`：
    - `synonym_index[synonym] = term_id`
- 用于快速查找：通过同义词文本查找对应的HPO术语ID

---

### 2.17 alternative_id_mapping（替代ID映射）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `alternative_id_mapping` | 替代ID映射（用于版本兼容：通过旧ID或替代ID查找新的标准ID） | object | 计算得出 | 遍历所有术语，将每个术语的 `alternative_ids` 作为key，术语ID作为value | `{"HP:0004715": "HP:0000003"}` |

**提取逻辑**：
- 不是直接从OWL源数据提取
- 遍历所有术语：
  - 对于每个 `alternative_id`：
    - `alternative_id_mapping[alt_id] = term_id`
- 用于版本兼容：通过旧ID查找新ID

---

### 2.18 deprecated_terms（废弃术语列表）

| 字段名 | 中文说明 | 数据类型 | 从OWL源数据提取位置 | 提取方法 | 示例值 |
|--------|----------|----------|---------------------|----------|--------|
| `deprecated_terms` | 废弃术语列表（所有已被废弃的术语ID列表，用于过滤和避免使用） | array[string] | 计算得出 | 遍历所有术语，找出 `deprecated` 为 `true` 的术语ID | `["HP:XXXXXX", ...]` |

**提取逻辑**：
- 不是直接从OWL源数据提取
- 遍历所有术语：
  - 如果 `term.deprecated == true`：
    - 将 `term.id` 添加到 `deprecated_terms` 数组
- 用于过滤：避免使用废弃的术语

---

## 三、提取优先级

### 3.1 必须提取（核心功能）

1. `id` - 术语标识符
2. `label` - 标准名称
3. `exact_synonyms` - 精确同义词（最重要，用于归一化）
4. `related_synonyms` - 相关同义词
5. `parent_ids` - 父类关系（用于层级结构）
6. `deprecated` - 废弃标记（避免使用废弃术语）

### 3.2 建议提取（重要功能）

7. `definition` - 定义（用于理解术语含义）
8. `child_ids` - 子类关系（计算得出）
9. `is_leaf` - 叶子节点标记（计算得出）
10. `alternative_ids` - 替代ID（版本兼容）
11. `replaced_by` - 被替代术语（版本迁移）
12. `xrefs` - 外部引用（与其他系统集成）

### 3.3 可选提取（辅助功能）

13. `comment` - 注释（补充说明）
14. `creation_date` - 创建日期（版本管理）

---

## 四、提取注意事项

### 4.1 带注解的属性（owl:Axiom）

某些属性可能在 `<owl:Axiom>` 中定义，需要关联提取：

```xml
<!-- 直接定义 -->
<oboInOwl:hasExactSynonym>Multicystic dysplastic kidney</oboInOwl:hasExactSynonym>

<!-- 带注解的定义 -->
<owl:Axiom>
    <owl:annotatedSource rdf:resource="http://purl.obolibrary.org/obo/HP_0000003"/>
    <owl:annotatedProperty rdf:resource="http://www.geneontology.org/formats/oboInOwl#hasExactSynonym"/>
    <owl:annotatedTarget>Multicystic dysplastic kidney</owl:annotatedTarget>
    <oboInOwl:hasSynonymType rdf:resource="http://purl.obolibrary.org/obo/hp#layperson"/>
</owl:Axiom>
```

**提取策略**：
- 先提取直接定义的属性
- 再提取 `<owl:Axiom>` 中关联的属性（通过 `annotatedSource` 关联）
- 合并去重

### 4.2 URI到ID的转换

OWL中使用URI，需要转换为HPO ID：

```
URI格式：http://purl.obolibrary.org/obo/HP_0000003
提取ID：HP:0000003

转换规则：
1. 提取URI中最后一个 `/` 后的部分：HP_0000003
2. 将下划线替换为冒号：HP:0000003
```

### 4.3 复杂关系表达式

某些 `rdfs:subClassOf` 可能指向 `<owl:Restriction>` 而不是直接的类：

```xml
<rdfs:subClassOf>
    <owl:Restriction>
        <owl:onProperty rdf:resource="..."/>
        <owl:someValuesFrom rdf:resource="http://purl.obolibrary.org/obo/HP_0000107"/>
    </owl:Restriction>
</rdfs:subClassOf>
```

**提取策略**：
- 如果 `rdfs:subClassOf` 直接指向类（`rdf:resource`），直接提取
- 如果指向 `Restriction`，需要解析 `Restriction` 结构，提取其中的类引用

### 4.4 多语言标签

`rdfs:label` 可能有多种语言：

```xml
<rdfs:label>Multicystic kidney dysplasia</rdfs:label>
<rdfs:label xml:lang="en">Multicystic kidney dysplasia</rdfs:label>
<rdfs:label xml:lang="zh">多囊性肾发育不良</rdfs:label>
```

**提取策略**：
- 优先提取 `xml:lang="en"` 的标签
- 如果没有英文标签，使用第一个标签
- 可选：同时提取中文标签作为额外字段

---

## 五、数据结构验证

### 5.1 必需字段检查

提取后需要验证：
- 每个术语必须有 `id` 和 `label`
- 每个术语的 `id` 必须以 `HP:` 开头
- `parent_ids` 中的每个ID必须存在于 `terms` 中

### 5.2 数据完整性检查

- `synonym_index` 中的每个key都应该有对应的value（HPO ID）
- `alternative_id_mapping` 中的每个key都应该有对应的value（HPO ID）
- `deprecated_terms` 中的每个ID都应该存在于 `terms` 中

---

## 六、总结

### 提取流程

```
1. 解析OWL文件
   ↓
2. 遍历所有HPO术语（从pathological phenotype observation开始）
   ↓
3. 对每个术语，按照映射表提取字段
   ↓
4. 计算派生字段（child_ids、is_leaf）
   ↓
5. 构建索引（synonym_index、alternative_id_mapping、deprecated_terms）
   ↓
6. 生成最终数据结构
```

