package com.aidoctor.diagnosis.runtime.effects;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Test-only isolated JVM probe for non-production durable reattachment. */
public final class CanonicalEffectLedgerProcessProbe {
    private CanonicalEffectLedgerProcessProbe() {}

    public static void main(String[] args) {
        if (args.length != 2) {
            System.exit(2);
        }
        Path root = Paths.get(args[0]);
        String mode = args[1];

        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);

        if ("write".equals(mode)) {
            CanonicalEffectLedgerDecision decision = ledger.createIfAbsent(
                    "U05_ADMISSION",
                    "effect-001",
                    "fingerprint-001",
                    "u05-admission-v1",
                    "canonical-payload".getBytes(StandardCharsets.UTF_8));
            if (!CanonicalEffectLedgerDecision.Status.CREATED.equals(decision.getStatus())
                    && !CanonicalEffectLedgerDecision.Status.REATTACHED.equals(decision.getStatus())) {
                System.exit(3);
            }
            System.exit(0);
        }

        if ("read".equals(mode)) {
            CanonicalEffectLedgerDecision decision =
                    ledger.inspect("U05_ADMISSION", "effect-001", "fingerprint-001");
            if (!CanonicalEffectLedgerDecision.Status.REATTACHED.equals(decision.getStatus())) {
                System.exit(4);
            }
            String payload = new String(
                    decision.getRecord().getPayload(),
                    StandardCharsets.UTF_8);
            if (!"canonical-payload".equals(payload)) {
                System.exit(5);
            }
            System.exit(0);
        }

        System.exit(6);
    }
}
