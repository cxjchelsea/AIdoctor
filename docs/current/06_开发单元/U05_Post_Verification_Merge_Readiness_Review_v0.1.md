# U05 Post-Verification Merge Readiness Review v0.1

> Scope: post-RDP-06 repository integration governance only  
> Verified SUT: `2b7926afd69de9fe2224d8a5b69e91c02c2db495`  
> Accepted verifier: `c334ea8ec6b75020c3bba12d02f48140a28b8318`  
> Closure/evidence head: `90c6ab379d625102ea7c78ae9289d0b7d7812b78`  
> Current main target: `6e68fd9fb7cd19e87aadae30f3bb53a2264d1920`  
> Status: **MERGE_READINESS_REVIEW_PENDING**

## 1. Verified state entering merge governance

```
U05 RDP-06 Authoritative Verification
= PASS

U05 Implementation Verification
= PASS

U05 authorized non-production verification scope
= CLOSED / VERIFIED

Merge Authorization
= NOT_GRANTED
```

Verification PASS is not merge authorization.

## 2. Repository topology

The exact verified SUT `2b7926...` is a direct descendant of current main `6e68fd9...`.

Comparison:

```
main -> verified SUT
status = ahead
ahead_by = 302
behind_by = 0
changed paths = 85
```

The accepted verifier `c334ea8e...` branches from the earlier U05 implementation lineage and is not an ancestor of the final SUT.

The closure head `90c6ab37...` is a direct descendant of the final SUT and contains only repository evidence/closure records.

Therefore a single blind merge of one existing PR cannot preserve all three accepted identities.

## 3. Main -> verified-SUT exact path inventory

Exact 85-path delta is classified as:

```
U05 production/runtime owned files = 38
reviewed shared production/support files = 5
tests = 6
governance/frozen docs = 35
non-production smoke workflow = 1
other = 0
```

No Spring/default production activation path is present.

No production live-routing configuration is present.

No live U06/U08/U10/U11/U14 execution wiring is present.

### Shared production/support paths

The five non-U05 production/support paths are:

```
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedger.java
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedgerDecision.java
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedgerRecord.java
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/NonProductionFileCanonicalEffectLedger.java
diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/SyntheticJsonPointerApplier.java
```

These are already part of the verified U05 target and were introduced through the separately governed shared-runtime PBNC-02A / Canonical Effect Ledger lineage.

They must not be re-authored during merge.

## 4. Required three-stage merge sequence

### M1 — Verified SUT lineage into main

Source identity:

`2b7926afd69de9fe2224d8a5b69e91c02c2db495`

Target identity at readiness review:

`main@6e68fd9fb7cd19e87aadae30f3bb53a2264d1920`

Required method:

`STANDARD MERGE COMMIT ONLY`

Prohibited:

`squash / rebase / cherry-pick reconstruction / force-update`

M1 brings the reviewed implementation/design/authorization/shared-capability lineage into main.

After M1, required checks:

```
main merge commit has exactly:
  parent1 = reviewed main target
  parent2 = exact verified SUT source

production tree for U05/shared reviewed paths
= verified source tree

Phase A/full diagnosis-service CI
= PASS

no production/live boundary activation
```

M2 cannot execute until M1 post-merge verification passes.

### M2 — Accepted verifier overlay into post-M1 main

Source identity:

`c334ea8ec6b75020c3bba12d02f48140a28b8318`

Expected delta relative to post-M1 main:

exactly the 15 reviewed verifier paths from PR #210.

No U05 production source is permitted in M2.

Required method:

`STANDARD MERGE COMMIT ONLY`

After M2, required checks:

```
15-path verifier inventory exact
accepted static digests unchanged
accepted verifier files byte-identical to c334ea8e...
production tree unchanged from post-M1
full CI = PASS
```

A new RDP-06 authoritative run is not automatically required merely because verifier-only files are merged, provided:
- implementation tree remains byte-identical to the already verified SUT;
- verifier files remain byte-identical to the accepted verifier;
- static digests remain exact;
- no verification semantic drift occurs.

Any drift reopens verification.

### M3 — Repository accepted evidence / closure into post-M2 main

Source closure identity:

`90c6ab379d625102ea7c78ae9289d0b7d7812b78`

M3 permitted content is exactly:

```
docs/current/06_开发单元/U05_Accepted_Verification_Evidence_v0.1.json
docs/current/06_开发单元/U05_RDP06_Implementation_Verification_Closure_v0.1.md
```

Required method:

`STANDARD MERGE COMMIT ONLY`

After M3:

```
accepted evidence snapshot exists in main
closure record exists in main
snapshot final verdict = PASS
combined review id = 5286165180
no runtime/source file changed by M3
```

## 5. Why the sequence is ordered

M1 must precede M2 because verifier evidence binds an exact implementation target.

M2 must precede M3 so the repository does not retain a final closure claiming an accepted verifier that is not yet repository-integrated.

Therefore:

```
M1 -> post-merge verification
-> M2 -> post-merge verification
-> M3 -> post-merge verification
```

is mandatory.

## 6. Main-target drift rule

The current M1 target is frozen for readiness review as:

`6e68fd9fb7cd19e87aadae30f3bb53a2264d1920`

If main moves before explicit M1 merge authorization:

```
STOP
-> compare new main against verified SUT
-> confirm no conflict/semantic drift
-> repeat merge-readiness target check
-> issue updated authorization package
```

No stale target authorization may be reused.

## 7. Authorization model

Recommended merge authorization model is staged, not one blanket authorization.

### M1 authorization

`AUTH-U05-M1-VERIFIED-SUT-MERGE-001`

Authorizes only exact verified SUT -> exact reviewed main target by standard merge commit.

### M2 authorization

Issued only after M1 post-merge verification passes.

### M3 authorization

Issued only after M2 post-merge verification passes.

This prevents a failure at one integration stage from implicitly authorizing later stages.

## 8. Production/live boundary

Even after M1/M2/M3 repository integration:

```
Production Authorization = BLOCKED
Production Clinical Runtime = NOT_ENABLED
Live upstream cutover = NOT_AUTHORIZED
Live downstream execution = NOT_AUTHORIZED
External delivery = NOT_AUTHORIZED
Release activation = NOT_AUTHORIZED
Real-patient traffic = NOT_AUTHORIZED
```

Repository integration does not activate clinical production behavior.

## 9. Current recommendation

Proceed with:

```
M1 exact merge candidate PR
-> independent merge-readiness review
-> CI / structural boundary checks
-> explicit owner M1 Merge Authorization Decision
```

Do not merge M2 or M3 yet.
