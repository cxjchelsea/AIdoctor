package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

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
 * Honest two-gate evidence:
 * (A) immutable historical Flyway V1..V6 is incompatible with disposable MySQL8;
 * (B) staged V7/V8 can migrate only from a SYNTHETIC database baselined at V6.
 * Neither constitutes a real existing production upgrade authorization.
 */
public class StageBHistoricalFlywayUpgradeTest {
    private final String pwd=System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD");

    private void guard() {
        Assertions.assertEquals("true",System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true",System.getenv("U07_F_ISOLATED"));
        Assertions.assertNotNull(pwd);
    }

    private String schema(String name) throws Exception {
        String schema="u07_d5o2_"+name;
        try(Connection db=DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:33331/?useSSL=false&allowPublicKeyRetrieval=true",
                "root",pwd);
            Statement statement=db.createStatement()) {
            statement.execute("CREATE DATABASE "+schema);
        }
        return "jdbc:mysql://127.0.0.1:33331/"+schema+
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private Path folder(boolean includeHistorical) throws Exception {
        Path directory=Files.createTempDirectory("u07-flyway-evidence-");
        if(includeHistorical) {
            Path source=Paths.get("src/main/resources/db/migration");
            for(int i=1;i<=6;i++) {
                final int v=i;
                try(Stream<Path> paths=Files.list(source)) {
                    Path sql=paths.filter(p->p.getFileName().toString()
                        .startsWith("V"+v+"__")).findFirst()
                        .orElseThrow(()->new IllegalStateException("U07_V_MISSING_"+v));
                    Files.copy(sql,directory.resolve(sql.getFileName()));
                }
            }
        } else {
            for(int version=7;version<=8;version++) {
                String name=version==7?"V7__u07_recovery_claim.sql":"V8__u07_outbox_no_dispatch.sql";
                Files.copy(Paths.get("../tools/u07_d5_o2_tx_topology/production_candidate/mysql8/"+name),
                    directory.resolve(name));
            }
        }
        return directory;
    }

    private static void cleanup(Path directory) throws Exception {
        try(Stream<Path> paths=Files.list(directory)) {
            paths.forEach(p->{try{Files.deleteIfExists(p);}
                catch(Exception e){throw new IllegalStateException(e);}});
        }
        Files.deleteIfExists(directory);
    }

    @Test
    public void originalHistoricalV1toV6MustNotBeMisreportedAsCompatible()throws Exception {
        guard();
        String url=schema("historical_raw");
        Path dir=folder(true);
        try {
            Flyway flyway=Flyway.configure().dataSource(url,"root",pwd)
                .locations("filesystem:"+dir.toAbsolutePath()).load();
            Exception failed=Assertions.assertThrows(Exception.class,flyway::migrate,
                "RAW_HISTORICAL_V1_TO_V6_UNEXPECTEDLY_PASSED_RECHECK_POLICY");
            String msg=String.valueOf(failed.getMessage());
            Assertions.assertTrue(msg.contains("V1") || msg.contains("Migration"),
                "EXPECTED_HISTORICAL_MIGRATION_FAILURE_NOT_OTHER_ERROR");
            System.out.println("U07_RAW_V1_TO_V6_MYSQL8=BLOCKED_EVIDENCE_CONFIRMED");
        } finally {
            cleanup(dir);
        }
    }

    @Test
    public void stagedV7V8MigrateFromSyntheticFlywaySixBaselineOnly()throws Exception {
        guard();
        String url=schema("staged_baseline6");
        // An *explicit synthetic* pre-existing table forces Flyway to record
        // an artificial V6 baseline. Not equivalent to real V1..V6 metadata.
        try(Connection db=DriverManager.getConnection(url,"root",pwd);
            Statement statement=db.createStatement()) {
            statement.execute("CREATE TABLE u07_test_baseline_marker (id INT PRIMARY KEY)");
        }
        Path dir=folder(false);
        try {
            Flyway flyway=Flyway.configure().dataSource(url,"root",pwd)
                .locations("filesystem:"+dir.toAbsolutePath())
                .baselineOnMigrate(true).baselineVersion("6").load();
            flyway.migrate();
            flyway.validate();
            Assertions.assertEquals("8",flyway.info().current().getVersion().getVersion());
            try(Connection db=DriverManager.getConnection(url,"root",pwd);
                Statement statement=db.createStatement()) {
                for(String name:new String[]{"u07_d5o2_recovery_claim","u07_d5o2_outbox_fence"}) {
                    try(java.sql.ResultSet rows=statement.executeQuery("SELECT COUNT(*) FROM "+name)) {
                        Assertions.assertTrue(rows.next());
                    }
                }
            }
            System.out.println("U07_STAGED_V7_V8_SYNTHETIC_BASELINE6=PASS_NOT_PRODUCTION");
        } finally {
            cleanup(dir);
        }
    }
}
