package com.aidoctor.verification.sourcebinding;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;
import java.text.Normalizer;
import java.util.*;

/** Test-only wire implementation. Names/ordering are RDP-01's exact 21 fields. */
final class BindingCodec {
    static final String PROFILE = "PROFILE-B-SYNTHETIC-STRUCTURAL-NONPROD";
    static final String ENV = "synthetic-source-binding-ci";
    static final String CONTRACT = "u07.inbound.v1";
    static final String[] NAMES = {"contract_version", "event_type", "trusted_environment_id",
        "trusted_profile_id", "trusted_tenant_scope_id", "trusted_consultation_id", "trusted_actor_scope_id",
        "question_id", "pending_question_ref", "parent_wait_effect_id", "resume_eligibility_id", "thread_id",
        "run_id", "checkpoint_id", "expected_clinical_state_version", "answer_payload_ref",
        "answer_payload_digest", "target_answer_event_id", "scope_authorization_ref", "actor_binding_ref", "payload_digest"};
    static String text(String v) {
        if (v == null) return null;
        String n = Normalizer.normalize(v, Normalizer.Form.NFC);
        if (n.isEmpty() || n.length() > 256 || n.codePoints().anyMatch(c -> Character.isISOControl(c)))
            throw new IllegalArgumentException("invalid framed text");
        // Reject malformed UTF-16 rather than silently replacing surrogates.
        try { StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).encode(CharBuffer.wrap(n)); }
        catch (CharacterCodingException e) { throw new IllegalArgumentException("invalid unicode", e); }
        return n;
    }
    static String id(String v) {
        if (v == null || !v.matches("synthetic-[a-z0-9._-]{1,100}")) throw new IllegalArgumentException("synthetic id required");
        return v;
    }
    static void item(DataOutputStream out, String v) throws IOException {
        if (v == null) { out.writeInt(-1); return; }
        byte[] b = text(v).getBytes(StandardCharsets.UTF_8); out.writeInt(b.length); out.write(b);
    }
    static byte[] frame(String prefix, String[] names, String[] values) {
        if (names.length != values.length) throw new IllegalArgumentException("field count");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes);
            item(out, prefix);
            for (int i = 0; i < names.length; i++) { item(out, names[i]); item(out, values[i]); }
            return bytes.toByteArray();
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    static String readItem(DataInputStream in) throws IOException {
        int size = in.readInt(); if (size == -1) return null;
        if (size < 1 || size > 2048) throw new IllegalArgumentException("invalid frame length");
        byte[] b = new byte[size]; in.readFully(b);
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b)).toString();
    }
    static String[] decode(byte[] b) {
        if (b == null || b.length > 32768) throw new IllegalArgumentException("invalid binding frame");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
            if (!"u07-event-binding-v1".equals(readItem(in))) throw new IllegalArgumentException("frame version");
            String[] f = new String[21];
            for (int i = 0; i < 21; i++) {
                if (!NAMES[i].equals(readItem(in))) throw new IllegalArgumentException("field order");
                f[i] = readItem(in);
            }
            if (in.available() != 0) throw new IllegalArgumentException("trailing bytes");
            validate(f);
            if (!Arrays.equals(b, encode(f))) throw new IllegalArgumentException("noncanonical bytes");
            return f;
        } catch (IOException e) { throw new IllegalArgumentException("broken frame", e); }
    }
    static byte[] encode(String[] fields) { validate(fields); return frame("u07-event-binding-v1", NAMES, fields); }
    static void validate(String[] f) {
        if (f == null || f.length != 21) throw new IllegalArgumentException("21 fields required");
        for (int i = 0; i < f.length; i++) {
            if (f[i] != null && !text(f[i]).equals(f[i])) throw new IllegalArgumentException("NFC required");
            if (f[i] == null && i != 13 && i != 15 && i != 16 && i != 17) throw new IllegalArgumentException("required field");
        }
        if (!CONTRACT.equals(f[0]) || !ENV.equals(f[2]) || !PROFILE.equals(f[3])) throw new IllegalArgumentException("unbound profile");
        for (int i : new int[]{4,5,6,7,8,9,10,11,12,18,19}) id(f[i]);
        if (f[13] != null) id(f[13]);
        if (!f[14].matches("0|[1-9][0-9]{0,9}") || Long.parseLong(f[14]) > Integer.MAX_VALUE) throw new IllegalArgumentException("version");
        if (!f[20].matches("[0-9a-f]{64}")) throw new IllegalArgumentException("payload digest");
        if ("USER_ANSWER".equals(f[1])) {
            if (f[15] == null || !f[15].matches("u07db:v1:u07-[0-9a-f]{64}:answer") || f[16] == null
                || !f[16].matches("[0-9a-f]{64}") || f[17] != null) throw new IllegalArgumentException("answer fields");
        } else if ("RESUME_REQUEST".equals(f[1])) {
            if (f[15] != null || f[16] != null) throw new IllegalArgumentException("resume payload forbidden");
            id(f[17]);
        } else throw new IllegalArgumentException("event type");
    }
    static String hash(byte[] b) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(b); StringBuilder s = new StringBuilder();
            for (byte v : h) s.append(String.format(Locale.ROOT, "%02x", v & 255)); return s.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static String key(String[] f, String token) {
        id(token);
        return "u07-" + hash(frame("u07-storage-key-v1", new String[]{"trusted_environment_id", "trusted_profile_id",
            "trusted_tenant_scope_id", "trusted_consultation_id", "trusted_actor_scope_id", "event_type", "ingress_supplied_stable_idempotency_token"},
            new String[]{f[2],f[3],f[4],f[5],f[6],f[1],token}));
    }
    static String payload(String[] f) {
        String[] names = "USER_ANSWER".equals(f[1]) ? new String[]{"event_type","answer_payload_ref","answer_payload_digest","payload_schema_version"}
                : new String[]{"event_type","target_answer_event_id","payload_schema_version"};
        String[] values = "USER_ANSWER".equals(f[1]) ? new String[]{f[1],f[15],f[16],"u07-payload-v1"}
                : new String[]{f[1],f[17],"u07-payload-v1"};
        return hash(frame("u07-payload-v1", names, values));
    }
}
