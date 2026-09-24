package com.aidoctor.diagnosis.runtime.u06;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public final class U06Ids {
    private U06Ids() {}
    public static String hash(String prefix, String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, prefix);
            for (String value : values) update(digest, value == null ? "<null>" : value);
            byte[] bytes = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b & 0xff));
            return prefix + "-" + hex.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
    private static void update(MessageDigest d, String v) { d.update(v.getBytes(StandardCharsets.UTF_8)); d.update((byte)0); }
}
