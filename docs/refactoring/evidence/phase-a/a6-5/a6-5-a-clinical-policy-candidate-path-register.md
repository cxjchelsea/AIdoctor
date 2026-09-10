# A6.5-A Clinical Policy Candidate Path Register

Document status: **Path-location only**
Task: `TASK-A03`
Batch: `A6.5-A`

## Boundary

Candidate register status: `REGISTERED_PATH_ONLY` (not clinically validated).

```text
Clinical content extracted: no
Clinical rules approved: 0
Thresholds approved: 0
Hypotheses approved: 0
Medical sources approved: 0
TASK-B04: BLOCKED
```

This register records repository-relative paths and metadata only.
It does not copy rule bodies, thresholds, prompt text, or medical conclusions.

## Candidates

### CLIN-020

- Asset ID: `CLIN-020`
- Repository-relative path: `health-state-assessment-service/app/rules/red_flag_rules.py`
- Path status: `TRACKED_FILE`
- Asset type: `CLINICAL_LOGIC_CODE`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `health-state-assessment-service`
- Reference paths: `docs/AI医生/项目文档/10.实现与部署/项目结构设计.md; docs/refactoring/evidence/phase-a/a2/python-clinical-asset-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; health-state-assessment-service/README.md; health-state-assessment-service/app/detectors/red_flag_detector.py; health-state-assessment-service/app/services/entry_assessment/step4_red_flag_check.py; health-state-assessment-service/docs/health-state-assessment-service - 已实现功能清单.md`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `QUARANTINE`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-060`

### CLIN-021

- Asset ID: `CLIN-021`
- Repository-relative path: `health-state-assessment-service/app/rules/risk_screening_rules.py`
- Path status: `TRACKED_FILE`
- Asset type: `CLINICAL_LOGIC_CODE`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `health-state-assessment-service`
- Reference paths: `docs/AI医生/项目文档/10.实现与部署/项目结构设计.md; docs/refactoring/evidence/phase-a/a2/python-clinical-asset-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; health-state-assessment-service/README.md; health-state-assessment-service/docs/health-state-assessment-service - 已实现功能清单.md`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `QUARANTINE`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-061`

### CLIN-022

- Asset ID: `CLIN-022`
- Repository-relative path: `health-state-assessment-service/app/rules/severity_rules.py`
- Path status: `TRACKED_FILE`
- Asset type: `CLINICAL_LOGIC_CODE`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `health-state-assessment-service`
- Reference paths: `docs/AI医生/项目文档/10.实现与部署/项目结构设计.md; docs/refactoring/evidence/phase-a/a2/python-clinical-asset-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; health-state-assessment-service/README.md; health-state-assessment-service/app/detectors/symptom_severity_detector.py; health-state-assessment-service/docs/health-state-assessment-service - 已实现功能清单.md; risk-assessment-service/app/services/risk_assessment_engine.py`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `QUARANTINE`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-062`

### CLIN-025

- Asset ID: `CLIN-025`
- Repository-relative path: `risk-assessment-service/app/services/triage_engine.py`
- Path status: `TRACKED_FILE`
- Asset type: `CLINICAL_LOGIC_CODE`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `risk-assessment-service`
- Reference paths: `docs/AI医生/项目文档/10.实现与部署/项目结构设计.md; docs/refactoring/evidence/phase-a/a2/python-clinical-asset-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; docs/方案讨论过程文件/AI医生系统-开发流程.md; risk-assessment-service/app/api/routes.py; risk-assessment-service/docs/risk-assessment-service - 已实现功能清单.md`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `QUARANTINE`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-065`

### CLIN-026

- Asset ID: `CLIN-026`
- Repository-relative path: `risk-assessment-service/app/services/upgrade_rule_engine.py`
- Path status: `TRACKED_FILE`
- Asset type: `CLINICAL_LOGIC_CODE`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `risk-assessment-service`
- Reference paths: `docs/AI医生/项目文档/10.实现与部署/项目结构设计.md; docs/refactoring/evidence/phase-a/a2/python-clinical-asset-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; docs/方案讨论过程文件/AI医生系统-开发流程.md; risk-assessment-service/app/api/routes.py; risk-assessment-service/app/services/risk_assessment_engine.py; risk-assessment-service/docs/risk-assessment-service - 已实现功能清单.md`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `QUARANTINE`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-066`

### PROMPT-001

- Asset ID: `PROMPT-001`
- Repository-relative path: `common/aidoctor_llm/prompt_manager.py`
- Path status: `TRACKED_FILE`
- Asset type: `PROMPT_ASSET`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `common`
- Reference paths: `common/aidoctor_llm/__init__.py; dialog-service/README.md; dialog-service/app/core/nlg.py; dialog-service/app/core/nlu.py; dialog-service/docs/dialog-service - 已实现功能清单.md; dialog-service/docs/dialog-service - 服务实现方案.md; docs/AI医生/项目文档/13.技术细节/LangChain架构开发流程.md; docs/refactoring/evidence/phase-a/a2/prompt-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; explanation-service/app/builders/explanation_generator.py; explanation-service/docs/explanation-service - 服务实现方案.md; health-state-assessment-service/app/services/entry_assessment/step2_enhanced/situation_judger.py; health-state-assessment-service/app/services/entry_assessment/step2_enhanced/symptom_concern_identifier.py; health-state-assessment-service/app/services/nlu/README.md; health-state-assessment-service/app/services/nlu/entity_extractor.py`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `NEEDS_DECISION`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-070`

### PROMPT-003

- Asset ID: `PROMPT-003`
- Repository-relative path: `diagnosis-engine-service/app/config/prompt_templates/diagnosis_reasoning.jinja2`
- Path status: `TRACKED_FILE`
- Asset type: `PROMPT_ASSET`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `diagnosis-engine-service`
- Reference paths: `common/aidoctor_llm/README.md; common/aidoctor_llm/prompt_manager.py; diagnosis-engine-service/app/config/prompt_templates/templates.yaml; diagnosis-engine-service/app/engines/llm_engine.py; diagnosis-engine-service/app/multi-engine-fusion/llm_engine.py; diagnosis-engine-service/app/utils/prompt_templates.py; diagnosis-engine-service/docs/diagnosis-engine-service - 已实现功能清单.md; docs/AI医生/项目文档/13.技术细节/LangChain架构开发流程.md; docs/refactoring/evidence/phase-a/a2/model-call-inventory.csv; docs/refactoring/evidence/phase-a/a2/prompt-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; docs/方案讨论过程文件/项目逻辑梳理/AI医生系统-大模型在推理流程中的作用与约束机制.md`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `NEEDS_DECISION`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-072`

### PROMPT-004

- Asset ID: `PROMPT-004`
- Repository-relative path: `diagnosis-engine-service/app/config/prompt_templates/path_injection.jinja2`
- Path status: `TRACKED_FILE`
- Asset type: `PROMPT_ASSET`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `diagnosis-engine-service`
- Reference paths: `common/aidoctor_llm/prompt_manager.py; diagnosis-engine-service/app/config/prompt_templates/templates.yaml; diagnosis-engine-service/app/utils/prompt_templates.py; docs/refactoring/evidence/phase-a/a2/model-call-inventory.csv; docs/refactoring/evidence/phase-a/a2/prompt-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; docs/方案讨论过程文件/项目逻辑梳理/AI医生系统-大模型在推理流程中的作用与约束机制.md`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `NEEDS_DECISION`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-073`

### PROMPT-008

- Asset ID: `PROMPT-008`
- Repository-relative path: `health-state-assessment-service/app/services/nlu/prompt_manager.py`
- Path status: `TRACKED_FILE`
- Asset type: `PROMPT_ASSET`
- Contains clinical logic: `YES` (declared metadata only)
- Current consumer paths: `health-state-assessment-service`
- Reference paths: `common/aidoctor_llm/__init__.py; dialog-service/README.md; dialog-service/app/core/nlg.py; dialog-service/app/core/nlu.py; dialog-service/docs/dialog-service - 已实现功能清单.md; dialog-service/docs/dialog-service - 服务实现方案.md; docs/AI医生/项目文档/13.技术细节/LangChain架构开发流程.md; docs/refactoring/evidence/phase-a/a2/prompt-inventory.csv; docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv; explanation-service/app/builders/explanation_generator.py; explanation-service/docs/explanation-service - 服务实现方案.md; health-state-assessment-service/app/services/entry_assessment/step2_enhanced/situation_judger.py; health-state-assessment-service/app/services/entry_assessment/step2_enhanced/symptom_concern_identifier.py; health-state-assessment-service/app/services/nlu/README.md; health-state-assessment-service/app/services/nlu/entity_extractor.py`
- Risk basis: `CLINICAL_LOGIC_ACTIVE_PATH`
- Current disposition: `NEEDS_DECISION`
- Owner role: `UNASSIGNED`
- Required reviewer role: `clinical`
- Blocking decision: `no_content_extraction_in_A6_5_A; TASK-B04_BLOCKED`
- Allowed next phase: `A6.5-B TASK-B04 content extraction only after unblock`
- Evidence ID: `DISC-077`
