package com.aidoctor.diagnosis.runtime.effects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Non-production filesystem-backed Canonical Effect Ledger.
 *
 * <p>Requires an explicit dedicated root. It has no Spring registration,
 * default production path, database, network, or business-aware serializer.</p>
 */
public final class NonProductionFileCanonicalEffectLedger implements CanonicalEffectLedger {
    private static final byte[] MAGIC = new byte[]{'A','I','D','E','F','F','0','1'};
    private static final int LEDGER_FORMAT_VERSION = 1;
    private static final int MAX_PAYLOAD_BYTES = 256 * 1024;
    private static final int MAX_NAMESPACE_LENGTH = 64;
    private static final int MAX_EFFECT_ID_LENGTH = 256;
    private static final int MAX_FINGERPRINT_LENGTH = 256;
    private static final int MAX_SCHEMA_VERSION_LENGTH = 128;
    private static final int MAX_RECORD_BYTES = MAX_PAYLOAD_BYTES + 32 * 1024;
    private static final Pattern OPAQUE =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]*$");

    /*
     * Java FileLock throws OverlappingFileLockException instead of blocking
     * when two threads in the same JVM lock the same region. Serialize the
     * bounded non-production publication critical section in-process, while
     * retaining FileLock for cross-process coordination.
     */
    private static final Object JVM_FILE_LOCK_COORDINATION = new Object();

    private final Path root;
    private final Clock clock;

    public NonProductionFileCanonicalEffectLedger(Path root) {
        this(root, Clock.systemUTC());
    }

    NonProductionFileCanonicalEffectLedger(Path root, Clock clock) {
        if (root == null) throw new IllegalArgumentException("ledger root is required");
        if (clock == null) throw new IllegalArgumentException("clock is required");
        this.clock = clock;
        this.root = initializeRoot(root);
    }

    @Override
    public CanonicalEffectLedgerDecision inspect(
            String namespace,
            String effectIdentity,
            String expectedCanonicalFingerprint) {
        validateNamespace(namespace);
        validateEffectIdentity(effectIdentity);
        validateFingerprint(expectedCanonicalFingerprint);

        try {
            StoragePaths paths = storagePaths(namespace, effectIdentity, false);
            if (paths == null) return CanonicalEffectLedgerDecision.absent();
            if (!safeExistingPath(paths.effectDirectory)
                    || !safeLeaf(paths.canonicalRecord)
                    || !safeLeaf(paths.lockFile)) {
                return CanonicalEffectLedgerDecision.unavailable("LEDGER_PATH_UNSAFE");
            }
            if (!Files.exists(paths.canonicalRecord, LinkOption.NOFOLLOW_LINKS)) {
                return CanonicalEffectLedgerDecision.absent();
            }
            ReadResult read = readCanonical(paths.canonicalRecord);
            if (read.unavailableReason != null) {
                return CanonicalEffectLedgerDecision.unavailable(read.unavailableReason);
            }
            if (read.corruptReason != null) {
                return CanonicalEffectLedgerDecision.corrupt(read.corruptReason);
            }
            CanonicalEffectLedgerRecord record = read.record;
            if (!namespace.equals(record.getNamespace())
                    || !effectIdentity.equals(record.getEffectIdentity())) {
                return CanonicalEffectLedgerDecision.corrupt("LEDGER_IDENTITY_MISMATCH");
            }
            ensureCanonicalDurability(paths.canonicalRecord, paths.effectDirectory);
            if (!expectedCanonicalFingerprint.equals(record.getCanonicalFingerprint())) {
                return CanonicalEffectLedgerDecision.conflict(
                        record, "LEDGER_CANONICAL_FINGERPRINT_CONFLICT");
            }
            return CanonicalEffectLedgerDecision.reattached(record);
        } catch (IOException exception) {
            return CanonicalEffectLedgerDecision.unavailable("LEDGER_INSPECT_IO_UNAVAILABLE");
        } catch (RuntimeException exception) {
            return CanonicalEffectLedgerDecision.unavailable("LEDGER_INSPECT_RUNTIME_UNAVAILABLE");
        }
    }

    @Override
    public CanonicalEffectLedgerDecision createIfAbsent(
            String namespace,
            String effectIdentity,
            String canonicalFingerprint,
            String recordSchemaVersion,
            byte[] immutableRecordBytes) {
        validateNamespace(namespace);
        validateEffectIdentity(effectIdentity);
        validateFingerprint(canonicalFingerprint);
        validateSchemaVersion(recordSchemaVersion);
        byte[] payload = validateAndCopyPayload(immutableRecordBytes);
        String payloadSha256 = sha256Hex(payload);

        Path temp = null;
        try {
            StoragePaths paths = storagePaths(namespace, effectIdentity, true);
            if (!safeExistingPath(paths.effectDirectory)
                    || !safeLeaf(paths.canonicalRecord)
                    || !safeLeaf(paths.lockFile)) {
                return CanonicalEffectLedgerDecision.unavailable("LEDGER_PATH_UNSAFE");
            }

            CanonicalEffectLedgerDecision existing = inspectExistingForCreate(
                    paths.canonicalRecord,
                    namespace,
                    effectIdentity,
                    canonicalFingerprint,
                    recordSchemaVersion,
                    payloadSha256);
            if (existing != null) return existing;

            CanonicalEffectLedgerRecord candidate = new CanonicalEffectLedgerRecord(
                    namespace,
                    effectIdentity,
                    canonicalFingerprint,
                    recordSchemaVersion,
                    payload,
                    payloadSha256,
                    clock.millis());
            byte[] encoded = encode(candidate);

            temp = Files.createTempFile(paths.effectDirectory, ".staging-", ".tmp");
            if (!contained(temp) || Files.isSymbolicLink(temp)) {
                return CanonicalEffectLedgerDecision.unavailable("LEDGER_TEMP_PATH_UNSAFE");
            }
            writeAndForce(temp, encoded);

            prepareLockFile(paths.lockFile);
            if (!safeLeaf(paths.lockFile)) {
                return CanonicalEffectLedgerDecision.unavailable("LEDGER_LOCK_PATH_UNSAFE");
            }

            synchronized (JVM_FILE_LOCK_COORDINATION) {
                try (FileChannel lockChannel = FileChannel.open(
                        paths.lockFile,
                        StandardOpenOption.WRITE);
                     FileLock ignored = lockChannel.lock()) {

                if (!safeExistingPath(paths.effectDirectory)
                        || !safeLeaf(paths.canonicalRecord)
                        || !safeLeaf(paths.lockFile)) {
                    return CanonicalEffectLedgerDecision.unavailable("LEDGER_PATH_UNSAFE");
                }

                existing = inspectExistingForCreate(
                        paths.canonicalRecord,
                        namespace,
                        effectIdentity,
                        canonicalFingerprint,
                        recordSchemaVersion,
                        payloadSha256);
                if (existing != null) return existing;

                try {
                    Files.move(temp, paths.canonicalRecord, StandardCopyOption.ATOMIC_MOVE);
                    temp = null;
                } catch (AtomicMoveNotSupportedException exception) {
                    return CanonicalEffectLedgerDecision.unavailable(
                            "LEDGER_ATOMIC_PUBLICATION_UNAVAILABLE");
                } catch (FileAlreadyExistsException race) {
                    existing = inspectExistingForCreate(
                            paths.canonicalRecord,
                            namespace,
                            effectIdentity,
                            canonicalFingerprint,
                            recordSchemaVersion,
                            payloadSha256);
                    return existing == null
                            ? CanonicalEffectLedgerDecision.unavailable("LEDGER_PUBLICATION_RACE_UNRESOLVED")
                            : existing;
                }

                if (!safeLeaf(paths.canonicalRecord)) {
                    return CanonicalEffectLedgerDecision.unavailable("LEDGER_CANONICAL_PATH_UNSAFE");
                }
                forceFile(paths.canonicalRecord);
                forceDirectory(paths.effectDirectory);

                ReadResult published = readCanonical(paths.canonicalRecord);
                if (published.unavailableReason != null) {
                    return CanonicalEffectLedgerDecision.unavailable(published.unavailableReason);
                }
                if (published.corruptReason != null) {
                    return CanonicalEffectLedgerDecision.corrupt(published.corruptReason);
                }
                if (!published.record.canonicalEquals(
                        namespace,
                        effectIdentity,
                        canonicalFingerprint,
                        recordSchemaVersion,
                        payloadSha256)) {
                    return CanonicalEffectLedgerDecision.corrupt(
                            "LEDGER_PUBLISHED_RECORD_MISMATCH");
                }
                    return CanonicalEffectLedgerDecision.created(published.record);
                } catch (IOException lockFailure) {
                    return CanonicalEffectLedgerDecision.unavailable(
                            "LEDGER_PUBLICATION_OR_COORDINATION_UNAVAILABLE");
                }
            }
        } catch (IOException exception) {
            return CanonicalEffectLedgerDecision.unavailable("LEDGER_CREATE_IO_UNAVAILABLE");
        } catch (RuntimeException exception) {
            return CanonicalEffectLedgerDecision.unavailable("LEDGER_CREATE_RUNTIME_UNAVAILABLE");
        } finally {
            if (temp != null) {
                try {
                    if (contained(temp) && !Files.isSymbolicLink(temp)) {
                        Files.deleteIfExists(temp);
                    }
                } catch (IOException ignored) {
                    // Orphan temp never becomes canonical authority.
                }
            }
        }
    }

    private CanonicalEffectLedgerDecision inspectExistingForCreate(
            Path canonicalRecord,
            String namespace,
            String effectIdentity,
            String canonicalFingerprint,
            String recordSchemaVersion,
            String payloadSha256) throws IOException {
        if (!safeLeaf(canonicalRecord)) {
            return CanonicalEffectLedgerDecision.unavailable("LEDGER_CANONICAL_PATH_UNSAFE");
        }
        if (!Files.exists(canonicalRecord, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        ReadResult read = readCanonical(canonicalRecord);
        if (read.unavailableReason != null) {
            return CanonicalEffectLedgerDecision.unavailable(read.unavailableReason);
        }
        if (read.corruptReason != null) {
            return CanonicalEffectLedgerDecision.corrupt(read.corruptReason);
        }
        CanonicalEffectLedgerRecord record = read.record;
        if (!namespace.equals(record.getNamespace())
                || !effectIdentity.equals(record.getEffectIdentity())) {
            return CanonicalEffectLedgerDecision.corrupt("LEDGER_IDENTITY_MISMATCH");
        }
        ensureCanonicalDurability(canonicalRecord, canonicalRecord.getParent());
        if (record.canonicalEquals(
                namespace,
                effectIdentity,
                canonicalFingerprint,
                recordSchemaVersion,
                payloadSha256)) {
            return CanonicalEffectLedgerDecision.reattached(record);
        }
        return CanonicalEffectLedgerDecision.conflict(
                record, "LEDGER_CANONICAL_RECORD_CONFLICT");
    }

    private StoragePaths storagePaths(
            String namespace,
            String effectIdentity,
            boolean createDirectories) throws IOException {
        Path namespaceDirectory = root.resolve(sha256Hex(
                namespace.getBytes(StandardCharsets.UTF_8)));
        Path effectDirectory = namespaceDirectory.resolve(sha256Hex(
                effectIdentity.getBytes(StandardCharsets.UTF_8)));

        if (createDirectories) {
            ensureSafeDirectory(namespaceDirectory);
            ensureSafeDirectory(effectDirectory);
        } else {
            if (!Files.exists(namespaceDirectory, LinkOption.NOFOLLOW_LINKS)) return null;
            if (!safeExistingPath(namespaceDirectory)) throw new IOException("unsafe namespace path");
            if (!Files.exists(effectDirectory, LinkOption.NOFOLLOW_LINKS)) return null;
            if (!safeExistingPath(effectDirectory)) throw new IOException("unsafe effect path");
        }

        return new StoragePaths(
                effectDirectory,
                effectDirectory.resolve("record.bin"),
                effectDirectory.resolve("create.lock"));
    }

    private void ensureSafeDirectory(Path directory) throws IOException {
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            if (!safeExistingPath(directory)) {
                throw new IOException("unsafe ledger directory");
            }
            return;
        }
        Files.createDirectory(directory);
        if (!safeExistingPath(directory)) {
            throw new IOException("unsafe ledger directory after create");
        }
    }

    private boolean safeExistingPath(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)
                || !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path real = path.toRealPath();
        return real.startsWith(root);
    }

    private boolean safeLeaf(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent == null || !safeExistingPath(parent)) return false;
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(path)) {
            return false;
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            Path real = path.toRealPath();
            return real.startsWith(root);
        }
        return path.toAbsolutePath().normalize().startsWith(root);
    }

    private boolean contained(Path path) {
        return path.toAbsolutePath().normalize().startsWith(root);
    }

    private void prepareLockFile(Path lockFile) throws IOException {
        if (Files.exists(lockFile, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(lockFile)) throw new IOException("lock file is symbolic link");
            return;
        }
        try {
            Files.createFile(lockFile);
        } catch (FileAlreadyExistsException race) {
            if (Files.isSymbolicLink(lockFile)) throw new IOException("lock file is symbolic link");
        }
    }

    private static Path initializeRoot(Path configuredRoot) {
        try {
            Path absolute = configuredRoot.toAbsolutePath().normalize();
            if (Files.exists(absolute, LinkOption.NOFOLLOW_LINKS)
                    && Files.isSymbolicLink(absolute)) {
                throw new IllegalArgumentException("ledger root must not be a symbolic link");
            }
            if (!Files.exists(absolute, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectories(absolute);
            }
            if (!Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(absolute)) {
                throw new IllegalArgumentException("ledger root must be a real directory");
            }
            return absolute.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("ledger root is unavailable", exception);
        }
    }

    private static void writeAndForce(Path file, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }

    private static void ensureCanonicalDurability(Path file, Path directory) throws IOException {
        forceFile(file);
        forceDirectory(directory);
    }

    private static void forceFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void forceDirectory(Path directory) throws IOException {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        }
    }

    private static byte[] encode(CanonicalEffectLedgerRecord record) throws IOException {
        ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
        DataOutputStream body = new DataOutputStream(bodyBuffer);
        body.write(MAGIC);
        body.writeInt(LEDGER_FORMAT_VERSION);
        writeUtf8(body, record.getNamespace());
        writeUtf8(body, record.getEffectIdentity());
        writeUtf8(body, record.getCanonicalFingerprint());
        writeUtf8(body, record.getRecordSchemaVersion());
        body.writeLong(record.getCreatedAtEpochMillis());
        writeUtf8(body, record.getPayloadSha256());
        byte[] payload = record.getPayload();
        body.writeInt(payload.length);
        body.write(payload);
        body.flush();

        byte[] bodyBytes = bodyBuffer.toByteArray();
        byte[] fullDigest = sha256(bodyBytes);

        ByteArrayOutputStream fileBuffer = new ByteArrayOutputStream();
        DataOutputStream file = new DataOutputStream(fileBuffer);
        file.writeInt(bodyBytes.length);
        file.write(bodyBytes);
        file.writeInt(fullDigest.length);
        file.write(fullDigest);
        file.flush();
        return fileBuffer.toByteArray();
    }

    private static ReadResult readCanonical(Path file) {
        try {
            long size = Files.size(file);
            if (size <= 0 || size > MAX_RECORD_BYTES) {
                return ReadResult.corrupt("LEDGER_RECORD_LENGTH_INVALID");
            }
            byte[] fileBytes = Files.readAllBytes(file);
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(fileBytes));
            int bodyLength = input.readInt();
            if (bodyLength <= 0 || bodyLength > MAX_RECORD_BYTES || bodyLength > fileBytes.length) {
                return ReadResult.corrupt("LEDGER_BODY_LENGTH_INVALID");
            }
            byte[] bodyBytes = new byte[bodyLength];
            input.readFully(bodyBytes);
            int digestLength = input.readInt();
            if (digestLength != 32) {
                return ReadResult.corrupt("LEDGER_FULL_DIGEST_LENGTH_INVALID");
            }
            byte[] storedDigest = new byte[digestLength];
            input.readFully(storedDigest);
            if (input.available() != 0 || !Arrays.equals(storedDigest, sha256(bodyBytes))) {
                return ReadResult.corrupt("LEDGER_FULL_DIGEST_MISMATCH");
            }

            DataInputStream body = new DataInputStream(new ByteArrayInputStream(bodyBytes));
            byte[] magic = new byte[MAGIC.length];
            body.readFully(magic);
            if (!Arrays.equals(MAGIC, magic)) {
                return ReadResult.corrupt("LEDGER_MAGIC_INVALID");
            }
            if (body.readInt() != LEDGER_FORMAT_VERSION) {
                return ReadResult.corrupt("LEDGER_FORMAT_VERSION_UNSUPPORTED");
            }
            String namespace = readUtf8(body, MAX_NAMESPACE_LENGTH);
            String effectIdentity = readUtf8(body, MAX_EFFECT_ID_LENGTH);
            String fingerprint = readUtf8(body, MAX_FINGERPRINT_LENGTH);
            String schemaVersion = readUtf8(body, MAX_SCHEMA_VERSION_LENGTH);
            long createdAt = body.readLong();
            String storedPayloadDigest = readUtf8(body, 64);
            int payloadLength = body.readInt();
            if (payloadLength <= 0 || payloadLength > MAX_PAYLOAD_BYTES) {
                return ReadResult.corrupt("LEDGER_PAYLOAD_LENGTH_INVALID");
            }
            byte[] payload = new byte[payloadLength];
            body.readFully(payload);
            if (body.available() != 0) {
                return ReadResult.corrupt("LEDGER_BODY_TRAILING_BYTES");
            }
            String calculatedPayloadDigest = sha256Hex(payload);
            if (!calculatedPayloadDigest.equals(storedPayloadDigest)) {
                return ReadResult.corrupt("LEDGER_PAYLOAD_DIGEST_MISMATCH");
            }

            validateNamespace(namespace);
            validateEffectIdentity(effectIdentity);
            validateFingerprint(fingerprint);
            validateSchemaVersion(schemaVersion);
            if (createdAt < 0) return ReadResult.corrupt("LEDGER_CREATED_AT_INVALID");

            return ReadResult.record(new CanonicalEffectLedgerRecord(
                    namespace,
                    effectIdentity,
                    fingerprint,
                    schemaVersion,
                    payload,
                    storedPayloadDigest,
                    createdAt));
        } catch (EOFException exception) {
            return ReadResult.corrupt("LEDGER_RECORD_TRUNCATED");
        } catch (IllegalArgumentException exception) {
            return ReadResult.corrupt("LEDGER_RECORD_FIELD_INVALID");
        } catch (IOException exception) {
            return ReadResult.unavailable("LEDGER_RECORD_IO_UNAVAILABLE");
        }
    }

    private static void writeUtf8(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readUtf8(DataInputStream input, int maxCharacters) throws IOException {
        int length = input.readInt();
        if (length <= 0 || length > maxCharacters * 4) {
            throw new IOException("invalid UTF-8 field length");
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        String value = new String(bytes, StandardCharsets.UTF_8);
        if (value.length() > maxCharacters) {
            throw new IOException("UTF-8 field exceeds character limit");
        }
        return value;
    }

    private static byte[] validateAndCopyPayload(byte[] payload) {
        if (payload == null || payload.length == 0 || payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("payload must contain 1..262144 bytes");
        }
        return Arrays.copyOf(payload, payload.length);
    }

    private static void validateNamespace(String value) {
        validateOpaque(value, MAX_NAMESPACE_LENGTH, "namespace");
    }

    private static void validateEffectIdentity(String value) {
        validateOpaque(value, MAX_EFFECT_ID_LENGTH, "effectIdentity");
    }

    private static void validateFingerprint(String value) {
        validateOpaque(value, MAX_FINGERPRINT_LENGTH, "canonicalFingerprint");
    }

    private static void validateSchemaVersion(String value) {
        validateOpaque(value, MAX_SCHEMA_VERSION_LENGTH, "recordSchemaVersion");
    }

    private static void validateOpaque(String value, int max, String name) {
        if (value == null || value.length() < 1 || value.length() > max || !OPAQUE.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " is malformed");
        }
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String sha256Hex(byte[] value) {
        return String.format("%064x", new BigInteger(1, sha256(value)));
    }

    private static final class StoragePaths {
        final Path effectDirectory;
        final Path canonicalRecord;
        final Path lockFile;

        StoragePaths(Path effectDirectory, Path canonicalRecord, Path lockFile) {
            this.effectDirectory = effectDirectory;
            this.canonicalRecord = canonicalRecord;
            this.lockFile = lockFile;
        }
    }

    private static final class ReadResult {
        final CanonicalEffectLedgerRecord record;
        final String corruptReason;
        final String unavailableReason;

        private ReadResult(
                CanonicalEffectLedgerRecord record,
                String corruptReason,
                String unavailableReason) {
            this.record = record;
            this.corruptReason = corruptReason;
            this.unavailableReason = unavailableReason;
        }

        static ReadResult record(CanonicalEffectLedgerRecord record) {
            return new ReadResult(record, null, null);
        }

        static ReadResult corrupt(String reason) {
            return new ReadResult(null, reason, null);
        }

        static ReadResult unavailable(String reason) {
            return new ReadResult(null, null, reason);
        }
    }
}
