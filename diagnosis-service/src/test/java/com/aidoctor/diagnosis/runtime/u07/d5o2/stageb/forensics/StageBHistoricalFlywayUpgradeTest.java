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

/** Runs the unmodified V1..V6 MySQL lineage then staged V7/V8 candidates. */
public class StageBHistoricalFlywayUpgradeTest {
    @Test public void originalV1toV6ThenV7V8UpgradeAndValidate() throws Exception {
        Assertions.assertEquals("true",System.getenv("GITHUB_ACTIONS"));
        Assertions.assertEquals("true",System.getenv("U07_F_ISOLATED"));
        String secret=System.getenv("U07_REPLAY_SYNTHETIC_ADMIN_PWD");
        Assertions.assertNotNull(secret);
        final String schema="u07_d5o2_historical_upgrade";
        String url="jdbc:mysql://127.0.0.1:33331/"+schema+
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try(Connection conn=DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:33331/?useSSL=false&allowPublicKeyRetrieval=true","root",secret);
            Statement statement=conn.createStatement()) {
            statement.execute("CREATE DATABASE "+schema);
        }
        Path original=Paths.get("src/main/resources/db/migration");
        Path folder=Files.createTempDirectory("u07-flyway-history-");
        try {
            for(int i=1;i<=6;i++) {
                final int version=i;
                final String prefix="V"+i+"__";
                try(Stream<Path> paths=Files.list(original)) {
                    Path source=paths.filter(p->p.getFileName().toString().startsWith(prefix))
                        .findFirst().orElseThrow(()->new IllegalStateException(
                            "U07_HISTORICAL_MIGRATION_MISSING_"+version));
                    Files.copy(source,folder.resolve(source.getFileName()));
                }
            }
            Flyway first=Flyway.configure().dataSource(url,"root",secret)
                .locations("filesystem:"+folder.toAbsolutePath()).load();
            first.migrate();
            Assertions.assertEquals("6",first.info().current().getVersion().getVersion());
            Files.copy(Paths.get("../tools/u07_d5_o2_tx_topology/production_candidate/mysql8/V7__u07_recovery_claim.sql"),
                folder.resolve("V7__u07_recovery_claim.sql"));
            Files.copy(Paths.get("../tools/u07_d5_o2_tx_topology/production_candidate/mysql8/V8__u07_outbox_no_dispatch.sql"),
                folder.resolve("V8__u07_outbox_no_dispatch.sql"));
            Flyway upgrade=Flyway.configure().dataSource(url,"root",secret)
                .locations("filesystem:"+folder.toAbsolutePath()).load();
            upgrade.validate();
            upgrade.migrate();
            upgrade.validate();
            Assertions.assertEquals("8",upgrade.info().current().getVersion().getVersion());
            try(Connection conn=DriverManager.getConnection(url,"root",secret);
                Statement statement=conn.createStatement()) {
                for(String table:new String[]{"u06_delivery_ledger","u07_d5o2_recovery_claim",
                    "u07_d5o2_outbox_fence"}) {
                    try(java.sql.ResultSet rs=statement.executeQuery("SELECT COUNT(*) FROM "+table)) {
                        Assertions.assertTrue(rs.next());
                    }
                }
            }
        } finally {
            try(Stream<Path> paths=Files.list(folder)) {
                paths.forEach(p->{try{Files.deleteIfExists(p);}
                    catch(Exception e){throw new IllegalStateException(e);}});
            }
            Files.deleteIfExists(folder);
        }
    }
}
