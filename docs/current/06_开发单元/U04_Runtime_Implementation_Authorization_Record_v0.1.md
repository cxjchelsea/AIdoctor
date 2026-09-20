# U04 Runtime Implementation Authorization Record v0.1

Authorization ID:

AUTH-U04-RUNTIME-IMPL-001

State:

AUTHORIZED / NON_PRODUCTION_ONLY / FROZEN_RDP01_TO_RDP06_ONLY / NO_LIVE_DOWNSTREAM_ROUTING

Authorization source:

Explicit repository-owner instruction in the project governance conversation: "好的，现在请开始实现U04".

Authorized implementation scope:

- U04 consumer admission for governed U03 outbound handoff;
- deterministic Safety Gate owner using the frozen RDP-02 policy;
- typed Safety Gate state proposal;
- controlled non-production G2 / StateCommitter commit adapter;
- downstream routing eligibility only;
- RDP-06 focused verification / durable evidence support;
- no additional Safety Capability;
- no fallback.

Frozen policy:

- VALID + NO_HIGH_RISK_SIGNAL -> ALLOW, only after admission/version/scope preconditions;
- VALID + CAUTION -> RESTRICTED;
- VALID + HIGH_RISK -> BLOCKED;
- U03 FAILED -> UNAVAILABLE;
- scope unavailable/not established -> UNAVAILABLE;
- known HIGH_RISK is preserved over technical/dependency failure.

Frozen dependency policy:

- NO_ADDITIONAL_REQUIRED_SAFETY_CAPABILITY_FOR_CURRENT_U04_V1_SLICE;
- OPTIONAL_SAFETY_CAPABILITY_SET = EMPTY;
- U04_V1_SAFETY_CAPABILITY_FALLBACK = NONE.

Not authorized:

- changes to RDP-02/RDP-05 semantics;
- new clinical/safety truth;
- new Safety Capability/model/tool/API;
- hidden fallback;
- live U05/U11/U14 execution;
- production U03->U04 routing;
- production mutation or release activation;
- real-patient traffic;
- unrelated runtime refactor;
- merge.

Implementation branch:

impl/u04-nonprod-safety-gate-v1

Governance base:

a5da7aefc8d9de247347336278258881635691aa

Authorization does not imply:

IMPLEMENTED
VERIFIED
INDEPENDENTLY_REVIEWED
MERGE_AUTHORIZED
MERGED
PMV_PASS
U04 ROUTING ACTIVATED
PRODUCTION AUTHORIZED
