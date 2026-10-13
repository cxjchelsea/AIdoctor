package com.aidoctor.diagnosis.runtime.u07.d5o2.mainexactprobe;

import com.aidoctor.diagnosis.runtime.u06.delivery.JdbcU06DeliveryStore;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Enumeration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test-only: never connects to a database or invokes a Store method. */
public final class ProbeStoreOriginTest {
    @Test
    public void loadedStoreMustBeFromPinnedHistoricalSource() throws Exception {
        Class<?> store = JdbcU06DeliveryStore.class;
        Path expectedRoot = Paths.get(required("PROBE_SOURCE_CLASSES")).toRealPath();
        Path actualRoot = Paths.get(store.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        Assertions.assertEquals(expectedRoot, actualRoot, "PROBE_STORE_CODESOURCE_MISMATCH");
        String resource = "com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.class";
        Enumeration<java.net.URL> paths = store.getClassLoader().getResources(resource);
        int count = 0;
        while (paths.hasMoreElements()) {
            paths.nextElement();
            count++;
        }
        Assertions.assertEquals(1, count, "PROBE_STORE_DUPLICATE_RESOURCE");
        String expectedHash = required("PROBE_STORE_CLASS_SHA256");
        Path compiledClass = expectedRoot.resolve(resource).toRealPath();
        byte[] bytes = Files.readAllBytes(compiledClass);
        Assertions.assertEquals(expectedHash, hex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                "PROBE_STORE_RUNTIME_BYTES_MISMATCH");
        try (InputStream in = store.getClassLoader().getResourceAsStream(resource)) {
            Assertions.assertNotNull(in, "PROBE_STORE_RESOURCE_MISSING");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) digest.update(buf, 0, n);
            Assertions.assertEquals(expectedHash, hex(digest.digest()), "PROBE_STORE_LOADED_BYTES_MISMATCH");
        }
        Assertions.assertEquals("33c0914e843d7976c7ff2272338377aa4925f443",
                required("PROBE_TARGET_MAIN_SHA"), "PROBE_SOURCE_SHA_MISMATCH");
        Assertions.assertEquals("6b67b75bb121410a0a78b6cbf188a5369ad5d1fb",
                required("PROBE_STORE_SOURCE_BLOB"), "PROBE_SOURCE_BLOB_MISMATCH");
        System.out.println("PROBE_ORIGINAL_STORE_RUNTIME_CLASS_AUTHORITY=PASS");
    }
    private static String required(String k) {
        String value = System.getenv(k);
        Assertions.assertNotNull(value, "PROBE_REQUIRED_ENV_MISSING_" + k);
        return value;
    }
    private static String hex(byte[] bytes) {
        char[] chars = "0123456789abcdef".toCharArray();
        StringBuilder b = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            b.append(chars[(value >>> 4) & 15]).append(chars[value & 15]);
        }
        return b.toString();
    }
}
