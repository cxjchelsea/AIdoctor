package com.aidoctor.diagnosis.runtime.u05;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stable opaque identifiers for the authorized U05 non-production slice. */
final class U05Ids {
    private U05Ids() {}

    static String hash(String prefix, String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, prefix);
            for (String value : values) {
                update(digest, value == null ? "<null>" : value);
            }
            byte[] bytes = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return prefix + "-" + hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
