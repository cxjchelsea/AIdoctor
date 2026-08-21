# POSTFREEZE-04 Java Raw OCR Controlled Cutover

Classification: NON_PRODUCTION_ENGINEERING_CONTROLLED_CUTOVER

Stage: POSTFREEZE-04

Authorized Base: `51e876d574dccd488b094ec2d244edd390c03adf`

Authorized Base tree: `dd4c4cbb3e5e28cedd227b638b0cb746d5f66b52`

Predecessor: POSTFREEZE-03D DURABLY_CLOSED

## Evidence question

Can a bounded diagnosis-service routing component
`ControlledRawOcrGateway`, under an explicit default-off server-side
typed policy, deliberately route `engineering.ocr.raw` through
`PythonRuntimeClient` to the durably verified canonical Python Runtime
controlled-opt-in path over real localhost HTTP with InputRef-only
and actual Tesseract, while ordinary Java/default/legacy OCR,
ToolCaller, AgentLoop, default `create_app()`, and
Clinical/Production boundaries remain unchanged, and Python Runtime
failure never silently falls back to legacy OCR?

## Work packages

WP-1: Bounded Java controlled routing (`ControlledRawOcrRoute`,
`ControlledRawOcrRoutePolicy`, `ControlledRawOcrGateway`,
`ControlledRawOcrGatewayException`, focused unit tests).

WP-2: Real cross-language application route proof
(`ControlledRawOcrGatewayRealToolIT`); one focused CI job.

## Exact 8-file scope

1. NEW `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/ControlledRawOcrRoute.java`
2. NEW `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/ControlledRawOcrRoutePolicy.java`
3. NEW `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/ControlledRawOcrGateway.java`
4. NEW `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/ControlledRawOcrGatewayException.java`
5. NEW `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/ControlledRawOcrGatewayTest.java`
6. NEW `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/ControlledRawOcrGatewayRealToolIT.java`
7. MODIFY `.github/workflows/ci.yml`
8. NEW this document

## Default-off policy

Safe/default route is `DISABLED`. Disabled invocation rejects before
any `PythonRuntimeClient` call. There is no automatic legacy fallback.

`JAVA_ROUTE_POLICY_DEFAULT = DISABLED`

`JAVA_PYTHON_ROUTE_DEFAULT_OFF = YES`

`POSTFREEZE_04_FALLBACK_POLICY = NO_AUTOMATIC_FALLBACK`

## Boundaries preserved

ToolCaller / AgentLoop wiring: OUT

examination-service default OCR route: UNCHANGED

Python source delta: 0

`PythonRuntimeClient.java` delta: 0

Shared Contracts schema/binding delta: 0

## Target evidence flag

`JAVA_PYTHON_RAW_OCR_CONTROLLED_CUTOVER_VERIFIED`

Authoring-time value: NO

Meaning after durable closure only: a bounded diagnosis-service
gateway, not wired into the ordinary application/agent path, can under
explicit server-side opt-in route the exact `engineering.ocr.raw / 0.0.1`
Shared Contracts `ToolContext` through existing `PythonRuntimeClient` to
the durable canonical Python Runtime controlled-opt-in path.

It does not mean Java default OCR replaced, examination-service OCR
replaced, Java/Python globally cut over, ToolCaller/AgentLoop Python
tool invocation, default Python Runtime Raw OCR enabled, Production,
clinical Runtime, or OCR quality.

## Authoring-time governance pointers (F004 preserved)

```
JAVA_PYTHON_RAW_OCR_CONTROLLED_CUTOVER_VERIFIED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
INDEPENDENT_REVIEW = NOT_PERFORMED
PMV = NOT_PERFORMED
MERGE_AUTHORIZATION = NO
POSTFREEZE_04_DURABLY_CLOSED = NO
F004 = PRESERVED
JAVA_PYTHON_RAW_OCR_CONTROLLED_CUTOVER_CI = PENDING_EXACT_HEAD_CI
```
