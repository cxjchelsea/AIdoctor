该文件夹包含 **ICD-10-CM 2026**（国际疾病分类第10版临床修改版）的官方数据文件，用于疾病和损伤的分类编码。

### 文件分类说明

#### 1. **索引文件（Index Files）**
- **`icd10cm-index-2026.xml`** (9.2MB)
  - 疾病和损伤的主索引，按字母顺序组织（A-Z）
  - 每个疾病条目关联对应的ICD-10-CM编码
  - 结构：字母 → 主术语 → 子术语 → ICD编码

- **`icd10cm_index_2026.pdf`** (41MB)
  - 索引的PDF版本，用于查阅和打印

- **`icd10cm-index-2026.xsd`** (6.2KB)
  - 索引XML文件的模式定义

#### 2. **表格列表文件（Tabular Files）**
- **`icd10cm-tabular-2026.xml`** (9.3MB)
  - 疾病编码的分类表格列表
  - 包含章节（Chapter）、节（Section）、诊断代码（Diagnosis Code）
  - 包含排除规则、包含说明、编码说明等

- **`icd10cm-tabular-2026.pdf`** (28MB)
  - 表格列表的PDF版本

- **`icd10cm-tabular-2026.xsd`** (12KB)
  - 表格XML文件的模式定义

#### 3. **药物索引文件（Drug Index）**
- **`icd10cm-drug-2026.xml`** (2.0MB)
  - 药物、物质和化学品的索引表
  - 7列：物质名称、意外中毒、自伤中毒、攻击中毒、未确定中毒、不良反应、用药不足
  - 每个物质在不同情况下的对应ICD编码

- **`icd10cm-drug-2026.pdf`** (3.1MB)
  - 药物索引的PDF版本

#### 4. **肿瘤索引文件（Neoplasm Index）**
- **`icd10cm-neoplasm-2026.xml`** (570KB)
  - 肿瘤相关疾病的索引表
  - 7列：肿瘤名称、恶性原发、恶性继发、原位癌、良性、不确定行为、未指定行为
  - 按身体部位和组织类型分类

- **`icd10cm-neoplasm-2026.pdf`** (4.5MB)
  - 肿瘤索引的PDF版本

- **`icd10cm-drug-neoplasm 2026.xsd`** (5.4KB)
  - 药物和肿瘤索引XML的模式定义

#### 5. **外部原因索引文件（External Cause Index）**
- **`icd10cm-eindex-2026.xml`** (1.0MB)
  - 外部原因索引，用于伤害和事故的外部原因编码
  - 包括：事故类型、运输方式、暴力原因等
  - 对应ICD-10-CM第20章（V00-Y99）

- **`icd10cm-eindex-2026.pdf`** (945KB)
  - 外部原因索引的PDF版本

### 数据格式说明

- XML 文件：结构化数据，便于程序解析和处理
- PDF 文件：便于人工查阅和打印
- XSD 文件：XML模式定义，描述数据结构

### 在AI医生系统中的应用价值

这些文件可用于：
1. 疾病编码标准化：将自然语言诊断转换为标准ICD-10-CM编码
2. 诊断匹配：通过索引快速查找疾病对应的编码
3. 知识库构建：提取疾病分类和编码规则
4. 临床术语标准化：统一疾病和症状的命名

这些是医疗信息系统中用于疾病分类和编码的核心参考数据。


## 什么是XML文件的模式定义

**XML模式定义（XML Schema Definition，简称XSD）**是一种定义XML文档结构和约束的规范文件。它像“蓝图”或“合同”，描述了：
- XML中允许哪些元素
- 元素的层次关系
- 元素和属性的数据类型
- 哪些是必需的，哪些是可选的
- 数据的取值范围和格式

### 类比理解

如果把XML文档比作实际的建筑，那么XSD就像是建筑图纸：
- XML = 实际的房屋（数据内容）
- XSD = 建筑图纸（结构规范）

### 实际例子

从ICD-10-CM的XSD文件中可以看到：

#### 1. 定义元素结构

```48:64:docs/AI医生/知识内容提取/源数据/icd10cm-table and index-2026/icd10cm-index-2026.xsd
    <xsd:element name="cell">
        <xsd:complexType mixed="true">
            <xsd:sequence>
                <xsd:choice minOccurs="0" maxOccurs="1">
                    <xsd:element ref="diff" />
                </xsd:choice>
            </xsd:sequence>
            <xsd:attribute name="col" use="required">
                <xsd:simpleType>
                    <xsd:restriction base="xsd:integer">
                        <xsd:minInclusive value="1"/>
                        <xsd:maxInclusive value="9"/>
                    </xsd:restriction>
                </xsd:simpleType>
            </xsd:attribute>
        </xsd:complexType>
    </xsd:element>
```

含义：
- `cell` 必须有一个 `col` 属性（required）
- `col` 必须是整数，范围在 1-9 之间

#### 2. 定义元素层次关系

```125:135:docs/AI医生/知识内容提取/源数据/icd10cm-table and index-2026/icd10cm-index-2026.xsd
    <xsd:element name="letter">
        <xsd:complexType>
            <xsd:sequence>
                <xsd:choice minOccurs="1" maxOccurs="1">
                    <xsd:element ref="title" />
                    <xsd:element ref="diff" />
                </xsd:choice>
                <xsd:element ref="mainTerm" minOccurs="1" maxOccurs="unbounded"/>
            </xsd:sequence>
        </xsd:complexType>
    </xsd:element>
```

含义：
- `letter` 必须包含一个 `title` 或 `diff`
- `letter` 必须包含至少一个 `mainTerm`，可以包含多个（unbounded）

#### 3. 定义顶级结构

```147:164:docs/AI医生/知识内容提取/源数据/icd10cm-table and index-2026/icd10cm-index-2026.xsd
    <xsd:element name="ICD10CM.index">
        <xsd:complexType>
            <xsd:sequence>
                <xsd:element ref="version" minOccurs="0" maxOccurs="1"/>
                <xsd:element ref="title" minOccurs="0" maxOccurs="1"/>
                <xsd:element ref="indexHeading" minOccurs="0" maxOccurs="1"/>
                <xsd:element ref="letter" minOccurs="1" maxOccurs="unbounded"/>
            </xsd:sequence>
            <xsd:attribute name="isAddenda">
                <xsd:simpleType>
                    <xsd:restriction base="xsd:string">
                        <xsd:enumeration value="true"/>
                        <xsd:enumeration value="false"/>
                    </xsd:restriction>
                </xsd:simpleType>
            </xsd:attribute>
        </xsd:complexType>
    </xsd:element>
```

含义：
- 根元素 `ICD10CM.index` 可以包含可选的 `version`、`title`、`indexHeading`
- 必须包含至少一个 `letter` 元素
- 可以有一个 `isAddenda` 属性，只能是 "true" 或 "false"

### XSD的主要作用

1. 验证XML文档：检查是否符合定义的规则
2. 文档化：作为XML结构的文档
3. 工具支持：IDE可以自动补全和检查
4. 数据交换：确保不同系统间数据格式一致

### 在您的项目中的应用

在ICD-10-CM数据中：
- `icd10cm-index-2026.xsd` 定义了索引XML的结构
- `icd10cm-tabular-2026.xsd` 定义了表格列表XML的结构
- `icd10cm-drug-neoplasm 2026.xsd` 定义了药物和肿瘤索引的结构

解析XML时，可以参考对应的XSD了解完整结构，避免遗漏字段或误用类型。

### 简单的对比示例

**没有XSD的XML**（可以随便写）：
```xml
<person>
  <name>张三</name>
  <age>abc</age>  <!-- 错误：年龄应该是数字 -->
</person>
```

**有XSD约束的XML**（必须符合规范）：
```xml
<person>
  <name>张三</name>  <!-- ✓ 符合 -->
  <age>abc</age>     <!-- ✗ 不符合：XSD要求age必须是整数 -->
</person>
```

XSD会在解析时检测出这种错误，确保数据的正确性和一致性。