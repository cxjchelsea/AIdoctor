package com.aidoctor.diagnosis.runtime.foundation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Brownfield guard for U01 and later new-runtime code.
 *
 * <p>The legacy diagnosis orchestration chain remains reachable for legacy-bound
 * consultations, but new runtime code must not grow dependencies back into that
 * chain. This is intentionally narrower than deletion: it enforces BLOCK_NEW_USE
 * while preserving rollback/compatibility behavior.</p>
 */
class U01LegacyBoundaryGuardTest {
    private static final List<String> FORBIDDEN_TOKENS = Arrays.asList(
            "DiagnosisOrchestrationService",
            "DiagnosisWorkflowOrchestrator",
            "WellnessScreeningOrchestrator",
            "HealthStateAssessmentClient",
            "CDPManager.updateCDP",
            ".executeDiagnosisWorkflow(",
            ".executeRemainingSteps(",
            ".step1IdentifyProblem(",
            "step1_identify_problem",
            "clinical_mode_collecting"
    );

    @Test
    void newRuntimeDoesNotDependOnLegacyDiagnosisAuthority() throws Exception {
        Path runtimeMain = moduleRoot().resolve("src/main/java/com/aidoctor/diagnosis/runtime");
        assertTrue(Files.isDirectory(runtimeMain), "runtime main package missing");

        final List<String> violations = new ArrayList<String>();
        Files.walkFileTree(runtimeMain, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toString().endsWith(".java")) {
                    return FileVisitResult.CONTINUE;
                }
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                for (String token : FORBIDDEN_TOKENS) {
                    if (source.contains(token)) {
                        violations.add(runtimeMain.relativize(file) + " contains forbidden legacy token: " + token);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertEquals(0, violations.size(), violations.toString());
    }

    private static Path moduleRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("src/main/java"))) {
            return current;
        }
        Path diagnosisService = current.resolve("diagnosis-service");
        if (Files.isDirectory(diagnosisService.resolve("src/main/java"))) {
            return diagnosisService;
        }
        throw new IllegalStateException("Cannot locate diagnosis-service module root from " + current);
    }
}
