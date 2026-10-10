package com.aidoctor.verification.sourcebinding;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

/** Deliberately package-private, tool-module only; never a real eligibility issuer. */
final class TestAuthority {
    static final String ISSUER = "synthetic-test-issuer-v1", POLICY = "synthetic-policy-v1";
    static final String MANIFEST = BindingCodec.hash("fixed-source-binding-test-manifest-v1".getBytes(StandardCharsets.UTF_8));
    static final String KEY_REF = "synthetic-aes-gcm-v1";
    private final JdbcTemplate issuer;
    private final TransactionTemplate tx;
    private final CipherBox box;
    TestAuthority(JdbcTemplate issuer, TransactionTemplate tx, CipherBox box, String profile) {
        if (!BindingCodec.PROFILE.equals(profile)) throw new IllegalArgumentException("test issuer only");
        this.issuer=issuer; this.tx=tx; this.box=box;
    }
    void epoch(String tenant, boolean allowed) {
        BindingCodec.id(tenant);
        tx.execute(s -> {
            issuer.update("INSERT IGNORE INTO u07_test_permission_epoch (tenant_id,epoch,allowed) VALUES (?,1,?)",tenant,allowed);
            issuer.queryForObject("SELECT epoch FROM u07_test_permission_epoch WHERE tenant_id=? FOR UPDATE",Long.class,tenant);
            issuer.update("UPDATE u07_test_permission_epoch SET epoch=epoch+1,allowed=? WHERE tenant_id=?",allowed,tenant);
            return null;
        });
    }
    void issue(String ref, String[] fields, String token, byte[] answer, Instant occurred) {
        BindingCodec.id(ref); BindingCodec.id(token); BindingCodec.validate(fields);
        if (occurred == null) throw new IllegalArgumentException("occurred_at required");
        String key=BindingCodec.key(fields,token);
        if (!BindingCodec.payload(fields).equals(fields[20])) throw new IllegalArgumentException("event payload");
        if ("USER_ANSWER".equals(fields[1])) {
            if (answer == null || answer.length>4096 || !new String(answer,StandardCharsets.UTF_8).startsWith("synthetic-answer:")
                || !BindingCodec.hash(answer).equals(fields[16]) || !("u07db:v1:"+key+":answer").equals(fields[15]))
                throw new IllegalArgumentException("nonclinical answer required");
        } else if (answer != null) throw new IllegalArgumentException("resume bytes forbidden");
        byte[] frame=BindingCodec.encode(fields), encrypted=answer == null ? null : box.encrypt(answer);
        tx.execute(s -> {
            List<Map<String,Object>> epochs=issuer.queryForList("SELECT epoch,allowed FROM u07_test_permission_epoch WHERE tenant_id=? FOR UPDATE",fields[4]);
            if(epochs.size()!=1 || ((Number)epochs.get(0).get("allowed")).intValue()!=1) throw new IllegalStateException("issuer denied");
            long epoch=((Number)epochs.get(0).get("epoch")).longValue();
            issuer.update("INSERT INTO u07_test_authority_record (record_id,record_type,action_id,issuer_id,policy_id,manifest_digest,tenant_id,permission_epoch,source_token,storage_key,binding_bytes,binding_fingerprint,answer_cipher,key_ref,occurred_at) VALUES (?,'SOURCE','SYNTHETIC_ADMISSION',?,?,?,?,?,?,?,?,?,?,?,?)",
                ref,ISSUER,POLICY,MANIFEST,fields[4],epoch,token,key,frame,BindingCodec.hash(frame),encrypted,encrypted==null?null:KEY_REF,occurred.toString());
            return null;
        });
    }
    static final class CipherBox {
        private final SecretKey key;
        CipherBox(byte[] fixtureKey) { key=new SecretKeySpec(fixtureKey.clone(),"AES"); }
        byte[] encrypt(byte[] plain) {
            try { byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
                c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));byte[] body=c.doFinal(plain);
                byte[] b=Arrays.copyOf(nonce,12+body.length);System.arraycopy(body,0,b,12,body.length);return b;
            } catch(GeneralSecurityException e){throw new IllegalStateException(e);}
        }
        byte[] decrypt(byte[] b, String ref) {
            if(b==null || b.length<28 || b.length>4124 || !KEY_REF.equals(ref)) throw new IllegalArgumentException("cipher reference");
            try {Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOf(b,12)));
                return c.doFinal(b,12,b.length-12);
            }catch(GeneralSecurityException e){throw new IllegalArgumentException("cipher integrity",e);}
        }
    }
}
