package com.aidoctor.diagnosis.runtime.u07.d5o2.stageb.forensics;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Disposable test ONLY: exercise real Flyway migrations V1..V8 on a fresh MySQL8 DB.
 * Generated V1 CLOB->LONGTEXT copy has a different checksum than frozen V1.
 * No production database or migration path is modified.
 */
public class StageBFreshMySQL8InstallCandidateTest {
    @Test public void generatedFreshInstallMigratesAllEightWithoutSyntheticBaseline() throws Exception {
        Assertions.assertEquals("true", System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true", System.getenv("U07_F_ISOLATED"));
        String folder=System.getenv("U07_FRESH_INSTALL_FIXTURE_DIR");
        String pwd=System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD");
        Assertions.assertNotNull(folder);
        Assertions.assertNotNull(pwd);
        Path migrationDir=Paths.get(folder).toAbsolutePath();
        Assertions.assertTrue(migrationDir.resolve("V7__u07_recovery_claim.sql").toFile().isFile());
        Assertions.assertTrue(migrationDir.resolve("V8__u07_outbox_no_dispatch.sql").toFile().isFile());
        String database="u07_fresh_candidate_full_v8";
        String rootUrl="jdbc:mysql://127.0.0.1:33331/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try(Connection root=DriverManager.getConnection(rootUrl,"root",pwd);
            Statement sql=root.createStatement()) {
            sql.execute("CREATE DATABASE "+database);
        }
        String jdbc="jdbc:mysql://127.0.0.1:33331/"+database+
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        Flyway flyway=Flyway.configure().dataSource(jdbc,"root",pwd)
            .locations("filesystem:"+migrationDir).baselineOnMigrate(false).load();
        flyway.migrate();
        flyway.validate();
        Assertions.assertEquals("8",flyway.info().current().getVersion().getVersion());
        try(Connection db=DriverManager.getConnection(jdbc,"root",pwd);
            Statement stmt=db.createStatement()) {
            for(String table:new String[]{"agent_state","audit_trail",
                    "clinical_runtime_binding","clinical_consultation",
                    "u07_d5o2_recovery_claim","u07_d5o2_outbox_fence"}) {
                try(ResultSet rows=stmt.executeQuery("SELECT COUNT(*) FROM "+table)) {
                    Assertions.assertTrue(rows.next());
                    Assertions.assertEquals(0,rows.getInt(1));
                }
            }
            try(ResultSet rows=stmt.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1")) {
                Assertions.assertTrue(rows.next());
                Assertions.assertEquals(8,rows.getInt(1));
            }
            try(ResultSet rows=stmt.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE type='BASELINE'")) {
                Assertions.assertTrue(rows.next());
                Assertions.assertEquals(0,rows.getInt(1));
            }
        }
        System.out.println("U07_FRESH_MYSQL8_FULL_V1_V8_FLYWAY7_NO_BASELINE=PASS");
    }
}
