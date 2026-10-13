package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Migration evidence only; never mutates frozen original V1..V6.
 * The CLOB-to-LONGTEXT compatibility transform is applied only to a
 * disposable copy, with exact-token and count assertions.
 */
public class StageBLegacyV1CompatibilityTest {
    private final String secret=System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD");
    private void guard() {
        Assertions.assertEquals("true",System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true",System.getenv("U07_F_ISOLATED"));
        Assertions.assertNotNull(secret);
    }
    private String database(String suffix)throws Exception {
        String schema="u07_v1_compat_"+suffix;
        try(Connection conn=DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:33331/?useSSL=false&allowPublicKeyRetrieval=true",
                "root",secret);
            Statement s=conn.createStatement()) {
            s.execute("CREATE DATABASE "+schema);
        }
        return "jdbc:mysql://127.0.0.1:33331/"+schema+
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
    private static Path prepare(boolean compatibilityCopy)throws Exception {
        Path folder=Files.createTempDirectory("u07-v1-compat-");
        Path root=Paths.get("src/main/resources/db/migration");
        for(int v=1;v<=6;v++) {
            final int version=v;
            Path original;
            try(Stream<Path> list=Files.list(root)) {
                original=list.filter(p->p.getFileName().toString()
                    .startsWith("V"+version+"__")).findFirst().orElseThrow(
                        ()->new IllegalStateException("MISSING_LEGACY_VERSION_"+version));
            }
            byte[] bytes=Files.readAllBytes(original);
            if(v==1 && compatibilityCopy) {
                String raw=new String(bytes,StandardCharsets.UTF_8);
                int occurrences=raw.split("\\bCLOB\\b",-1).length-1;
                Assertions.assertEquals(9,occurrences,"STRICT_CLOB_TOKEN_INVENTORY");
                // The source file and deployed Flyway checksums remain untouched.
                bytes=raw.replaceAll("\\bCLOB\\b","LONGTEXT")
                    .getBytes(StandardCharsets.UTF_8);
            }
            Files.write(folder.resolve(original.getFileName()),bytes);
        }
        return folder;
    }
    private static void clean(Path folder)throws Exception {
        try(Stream<Path> files=Files.list(folder)) {
            files.forEach(p->{try{Files.deleteIfExists(p);}
                catch(Exception failure){throw new IllegalStateException(failure);}});
        }
        Files.deleteIfExists(folder);
    }
    @Test public void exactFrozenV1FailsWithoutAnyRepair()throws Exception {
        guard();
        Path folder=prepare(false);
        try {
            Flyway flyway=Flyway.configure().dataSource(database("raw"),"root",secret)
                .locations("filesystem:"+folder.toAbsolutePath()).load();
            Exception failure=Assertions.assertThrows(Exception.class,flyway::migrate);
            Assertions.assertTrue(String.valueOf(failure.getMessage()).contains("V1"),
                "UNEXPECTED_FAILURE_BEFORE_V1");
            System.out.println("U07_EXACT_FROZEN_V1_MYSQL8=BLOCKED");
        } finally {clean(folder);}
    }
    @Test public void isolatedCompatibilityCopyMigratesFullHistoricalLine()throws Exception {
        guard();
        Path folder=prepare(true);
        try {
            Flyway f=Flyway.configure().dataSource(database("copy"),"root",secret)
                .locations("filesystem:"+folder.toAbsolutePath()).load();
            f.migrate();
            f.validate();
            Assertions.assertEquals("6",f.info().current().getVersion().getVersion());
            System.out.println("U07_ISOLATED_CLOB_LONGTEXT_COMPAT_V1_V6=PASS");
        } finally {clean(folder);}
    }
}
