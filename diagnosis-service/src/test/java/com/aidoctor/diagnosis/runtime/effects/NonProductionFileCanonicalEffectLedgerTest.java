package com.aidoctor.diagnosis.runtime.effects;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NonProductionFileCanonicalEffectLedgerTest {
    private static final String NS = "U05_ADMISSION";
    private static final String ID = "effect-001";
    private static final String FP = "fingerprint-001";
    private static final String SCHEMA = "u05-admission-v1";
    private static final byte[] PAYLOAD = "canonical-payload".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path root;

    @Test
    void createThenInspectReattachesExactRecord() {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);

        CanonicalEffectLedgerDecision created =
                ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);
        CanonicalEffectLedgerDecision inspected =
                ledger.inspect(NS, ID, FP);

        assertEquals(CanonicalEffectLedgerDecision.Status.CREATED, created.getStatus());
        assertEquals(CanonicalEffectLedgerDecision.Status.REATTACHED, inspected.getStatus());
        assertArrayEquals(PAYLOAD, inspected.getRecord().getPayload());
        assertEquals(created.getRecord().getPayloadSha256(),
                inspected.getRecord().getPayloadSha256());
        assertEquals(created.getRecord().getCreatedAtEpochMillis(),
                inspected.getRecord().getCreatedAtEpochMillis());
    }

    @Test
    void exactReplayDoesNotRewriteCanonicalRecord() throws Exception {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        CanonicalEffectLedgerDecision first =
                ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);
        Path record = recordPath(root);
        long modified = Files.getLastModifiedTime(record).toMillis();

        Thread.sleep(5L);
        CanonicalEffectLedgerDecision replay =
                ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);

        assertEquals(CanonicalEffectLedgerDecision.Status.REATTACHED, replay.getStatus());
        assertEquals(first.getRecord().getCreatedAtEpochMillis(),
                replay.getRecord().getCreatedAtEpochMillis());
        assertEquals(modified, Files.getLastModifiedTime(record).toMillis());
    }

    @Test
    void sameIdentityDifferentFingerprintConflicts() {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);

        CanonicalEffectLedgerDecision conflict =
                ledger.createIfAbsent(NS, ID, "fingerprint-002", SCHEMA, PAYLOAD);

        assertEquals(CanonicalEffectLedgerDecision.Status.CONFLICT, conflict.getStatus());
        assertArrayEquals(PAYLOAD, conflict.getRecord().getPayload());
    }

    @Test
    void sameIdentityAndFingerprintDifferentSchemaConflicts() {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);

        CanonicalEffectLedgerDecision conflict =
                ledger.createIfAbsent(NS, ID, FP, "u05-admission-v2", PAYLOAD);

        assertEquals(CanonicalEffectLedgerDecision.Status.CONFLICT, conflict.getStatus());
    }

    @Test
    void sameIdentityFingerprintSchemaDifferentPayloadConflicts() {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);

        CanonicalEffectLedgerDecision conflict =
                ledger.createIfAbsent(
                        NS,
                        ID,
                        FP,
                        SCHEMA,
                        "different-payload".getBytes(StandardCharsets.UTF_8));

        assertEquals(CanonicalEffectLedgerDecision.Status.CONFLICT, conflict.getStatus());
    }

    @Test
    void serviceObjectReconstructionReattachesFromSameRoot() {
        NonProductionFileCanonicalEffectLedger first =
                new NonProductionFileCanonicalEffectLedger(root);
        assertEquals(
                CanonicalEffectLedgerDecision.Status.CREATED,
                first.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD).getStatus());

        NonProductionFileCanonicalEffectLedger reconstructed =
                new NonProductionFileCanonicalEffectLedger(root);
        CanonicalEffectLedgerDecision decision =
                reconstructed.inspect(NS, ID, FP);

        assertEquals(CanonicalEffectLedgerDecision.Status.REATTACHED, decision.getStatus());
        assertArrayEquals(PAYLOAD, decision.getRecord().getPayload());
    }

    @Test
    void isolatedJvmWriterAndReaderReattachSameRecord() throws Exception {
        Process writer = probe(root, "write");
        assertEquals(0, writer.waitFor());

        Process reader = probe(root, "read");
        assertEquals(0, reader.waitFor());
    }

    @Test
    void corruptCanonicalRecordFailsClosedWithoutOverwrite() throws Exception {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);
        Path record = recordPath(root);
        byte[] bytes = Files.readAllBytes(record);
        bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] ^ 0x01);
        Files.write(record, bytes);

        CanonicalEffectLedgerDecision inspected = ledger.inspect(NS, ID, FP);
        CanonicalEffectLedgerDecision createRetry =
                ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);

        assertEquals(CanonicalEffectLedgerDecision.Status.CORRUPT, inspected.getStatus());
        assertEquals(CanonicalEffectLedgerDecision.Status.CORRUPT, createRetry.getStatus());
    }

    @Test
    void namespacesIsolateSameEffectIdentity() {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);

        CanonicalEffectLedgerDecision left =
                ledger.createIfAbsent("U05_ADMISSION", ID, FP, SCHEMA, PAYLOAD);
        CanonicalEffectLedgerDecision right =
                ledger.createIfAbsent("U05_ROUTING_DECISION", ID, FP, SCHEMA, PAYLOAD);

        assertEquals(CanonicalEffectLedgerDecision.Status.CREATED, left.getStatus());
        assertEquals(CanonicalEffectLedgerDecision.Status.CREATED, right.getStatus());
    }

    @Test
    void callerAndReturnedPayloadMutationCannotChangeCanonicalRecord() {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        byte[] caller = "immutable".getBytes(StandardCharsets.UTF_8);

        CanonicalEffectLedgerDecision created =
                ledger.createIfAbsent(NS, ID, FP, SCHEMA, caller);
        caller[0] = 'X';
        byte[] returned = created.getRecord().getPayload();
        returned[0] = 'Y';

        CanonicalEffectLedgerDecision reattached = ledger.inspect(NS, ID, FP);
        assertArrayEquals(
                "immutable".getBytes(StandardCharsets.UTF_8),
                reattached.getRecord().getPayload());
    }

    @Test
    void oversizedPayloadFailsBeforeCanonicalCreation() {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        final byte[] oversized = new byte[(256 * 1024) + 1];

        assertThrows(IllegalArgumentException.class,
                () -> ledger.createIfAbsent(NS, ID, FP, SCHEMA, oversized));
        assertEquals(
                CanonicalEffectLedgerDecision.Status.ABSENT,
                ledger.inspect(NS, ID, FP).getStatus());
    }

    @Test
    void orphanTempFileIsNeverCanonicalAuthority() throws Exception {
        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        Path effectDirectory = root
                .resolve(sha256Hex(NS))
                .resolve(sha256Hex(ID));
        Files.createDirectories(effectDirectory);
        Files.write(
                effectDirectory.resolve(".staging-orphan.tmp"),
                "orphan".getBytes(StandardCharsets.UTF_8));

        CanonicalEffectLedgerDecision inspected = ledger.inspect(NS, ID, FP);

        assertEquals(CanonicalEffectLedgerDecision.Status.ABSENT, inspected.getStatus());
    }

    @Test
    void precreatedSymlinkStoragePathFailsClosed() throws Exception {
        Path outside = Files.createTempDirectory(root.getParent(), "ledger-outside-");
        Path namespaceDirectory = root.resolve(sha256Hex(NS));
        try {
            Files.createSymbolicLink(namespaceDirectory, outside);
        } catch (UnsupportedOperationException | IOException | SecurityException exception) {
            Assumptions.assumeTrue(false, "symbolic links unavailable on this test platform");
        }

        NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        CanonicalEffectLedgerDecision decision =
                ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);

        assertEquals(CanonicalEffectLedgerDecision.Status.UNAVAILABLE, decision.getStatus());
        assertEquals(0L, Files.list(outside).count());
    }

    @Test
    void concurrentExactCreatorsHaveOneCreatedAndRestReattached() throws Exception {
        final NonProductionFileCanonicalEffectLedger ledger =
                new NonProductionFileCanonicalEffectLedger(root);
        final int workers = 8;
        final CountDownLatch ready = new CountDownLatch(workers);
        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<CanonicalEffectLedgerDecision>> futures =
                    new ArrayList<Future<CanonicalEffectLedgerDecision>>();
            for (int index = 0; index < workers; index++) {
                futures.add(executor.submit(new Callable<CanonicalEffectLedgerDecision>() {
                    @Override
                    public CanonicalEffectLedgerDecision call() throws Exception {
                        ready.countDown();
                        start.await();
                        return ledger.createIfAbsent(NS, ID, FP, SCHEMA, PAYLOAD);
                    }
                }));
            }
            ready.await();
            start.countDown();

            int created = 0;
            int reattached = 0;
            for (Future<CanonicalEffectLedgerDecision> future : futures) {
                CanonicalEffectLedgerDecision decision = future.get();
                if (CanonicalEffectLedgerDecision.Status.CREATED.equals(decision.getStatus())) created++;
                if (CanonicalEffectLedgerDecision.Status.REATTACHED.equals(decision.getStatus())) reattached++;
            }
            assertEquals(1, created);
            assertEquals(workers - 1, reattached);
        } finally {
            executor.shutdownNow();
        }
    }

    private static Process probe(Path root, String mode) throws IOException {
        String java = System.getProperty("java.home")
                + java.io.File.separator + "bin"
                + java.io.File.separator + "java";
        String classpath = System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path"));
        return new ProcessBuilder(
                java,
                "-cp",
                classpath,
                CanonicalEffectLedgerProcessProbe.class.getName(),
                root.toAbsolutePath().toString(),
                mode)
                .redirectErrorStream(true)
                .inheritIO()
                .start();
    }

    private static Path recordPath(Path root) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(path -> "record.bin".equals(path.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("record.bin not found"));
        }
    }

    private static String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : digest) result.append(String.format("%02x", item & 0xff));
        return result.toString();
    }
}
