package com.aidoctor.diagnosis.state.committer;

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
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateCommitterArchitectureGuardTest {
    private static final List<String> FORBIDDEN_ANNOTATIONS = Arrays.asList(
            "@Component",
            "@Service",
            "@Configuration",
            "@RestController",
            "@Controller",
            "@Bean",
            "@ConditionalOnProperty",
            "@FeignClient",
            "@Repository"
    );
    private static final List<String> FORBIDDEN_IMPORTS = Arrays.asList(
            "org.springframework.stereotype",
            "org.springframework.context.annotation",
            "org.springframework.web.bind",
            "org.springframework.cloud.openfeign",
            "javax.persistence.EntityManager",
            "org.springframework.data.jpa",
            "com.aidoctor.diagnosis.service.cdp.CDPManager",
            "com.aidoctor.diagnosis.client.",
            "com.aidoctor.diagnosis.controller."
    );
    private static final List<String> FORBIDDEN_CALLS = Arrays.asList(
            "CDPManager.update",
            "EntityManager",
            "Repository.save",
            "RestTemplate",
            "WebClient",
            "ModelGateway",
            "openai",
            "langgraph"
    );
    private static final List<String> PRODUCTION_WIRE_TARGETS = Arrays.asList(
            "src/main/java/com/aidoctor/diagnosis/agent/AgentLoop.java",
            "src/main/java/com/aidoctor/diagnosis/agent/ClinicalAgentBrain.java",
            "src/main/java/com/aidoctor/diagnosis/agent/ToolCaller.java",
            "src/main/java/com/aidoctor/diagnosis/service/cdp/CDPManager.java",
            "src/main/java/com/aidoctor/diagnosis/controller/DiagnosisController.java",
            "src/main/java/com/aidoctor/diagnosis/service/DiagnosisOrchestrationService.java",
            "src/main/java/com/aidoctor/diagnosis/service/orchestration/DiagnosisWorkflowOrchestrator.java"
    );
    private static final List<String> FORBIDDEN_CONTENT_TOKENS = Arrays.asList(
            "cough",
            "dyspnea",
            "pneumonia",
            "red flag",
            "red_flag",
            "spo2",
            "treatment",
            "medication",
            "mimic",
            "eicu"
    );

    @Test
    void stateCommitterHasNoSpringOrPersistenceWiring() throws Exception {
        Path committerMain = moduleRoot().resolve("src/main/java/com/aidoctor/diagnosis/state/committer");
        assertTrue(Files.isDirectory(committerMain), "committer main package missing");
        List<String> violations = new ArrayList<String>();
        for (Path file : javaFiles(committerMain)) {
            String source = read(file);
            for (int index = 0; index < FORBIDDEN_ANNOTATIONS.size(); index++) {
                if (source.contains(FORBIDDEN_ANNOTATIONS.get(index))) {
                    violations.add(file.getFileName() + " contains " + FORBIDDEN_ANNOTATIONS.get(index));
                }
            }
            for (int index = 0; index < FORBIDDEN_IMPORTS.size(); index++) {
                if (source.contains(FORBIDDEN_IMPORTS.get(index))) {
                    violations.add(file.getFileName() + " imports " + FORBIDDEN_IMPORTS.get(index));
                }
            }
            for (int index = 0; index < FORBIDDEN_CALLS.size(); index++) {
                if (source.contains(FORBIDDEN_CALLS.get(index))) {
                    violations.add(file.getFileName() + " references " + FORBIDDEN_CALLS.get(index));
                }
            }
        }
        assertEquals(0, violations.size(), violations.toString());
    }

    @Test
    void toolLlmAndAgentHaveNoStateCommitterProductionWiring() throws Exception {
        Path module = moduleRoot();
        List<String> violations = new ArrayList<String>();
        for (int index = 0; index < PRODUCTION_WIRE_TARGETS.size(); index++) {
            Path file = module.resolve(PRODUCTION_WIRE_TARGETS.get(index));
            assertTrue(Files.isRegularFile(file), "missing " + file);
            String source = read(file);
            if (source.contains("state.committer")
                    || source.contains("StateCommitter")
                    || source.contains("SyntheticVersionedStateRepository")
                    || source.contains("StateRepositoryPort")) {
                violations.add(PRODUCTION_WIRE_TARGETS.get(index));
            }
        }
        assertEquals(0, violations.size(), violations.toString());
    }

    @Test
    void productionSourcesDoNotDirectlyConstructSyntheticStateRepository() throws Exception {
        Path productionRoot = moduleRoot().resolve("src/main/java/com/aidoctor/diagnosis");
        Path committerRoot = moduleRoot().resolve("src/main/java/com/aidoctor/diagnosis/state/committer");
        List<String> violations = new ArrayList<String>();
        for (Path file : javaFiles(productionRoot)) {
            if (file.startsWith(committerRoot)) {
                continue;
            }
            String source = read(file);
            if (source.contains("SyntheticVersionedStateRepository")
                    || source.contains("new StateRepositoryPort.AtomicCommitCommand")
                    || source.contains(".attemptAtomicCommit(")) {
                violations.add(file.toString());
            }
        }
        assertEquals(0, violations.size(), violations.toString());
    }

    @Test
    void syntheticFixturesHaveZeroClinicalPhiAndProviderContent() throws Exception {
        Path committerRoot = moduleRoot().resolve("src");
        final int[] counts = new int[] {0, 0, 0, 0};
        final List<String> hits = new ArrayList<String>();
        Files.walkFileTree(committerRoot.resolve("main/java/com/aidoctor/diagnosis/state/committer"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                collectContentHits(file, hits);
                return FileVisitResult.CONTINUE;
            }
        });
        Files.walkFileTree(committerRoot.resolve("test/java/com/aidoctor/diagnosis/state/committer"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                collectContentHits(file, hits);
                return FileVisitResult.CONTINUE;
            }
        });
        assertEquals(0, hits.size(), hits.toString());
        assertEquals(0, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(0, counts[2]);
        assertEquals(0, counts[3]);
    }

    @Test
    void noSharedContractForkTypesExist() throws Exception {
        Path committerMain = moduleRoot().resolve("src/main/java/com/aidoctor/diagnosis/state/committer");
        String joined = joinJava(committerMain);
        assertFalse(joined.contains("class StatePatchV2"));
        assertFalse(joined.contains("class InternalStatePatch"));
        assertFalse(joined.contains("class ExpectedVersionPatch"));
        assertFalse(joined.contains("class ClinicalStatePatch"));
        assertFalse(joined.contains("class EncounterCDP"));
        assertFalse(joined.contains("class ClinicalObservation"));
        assertFalse(Pattern.compile("class\\s+AgentEvent\\b").matcher(joined).find());
    }

    private static void collectContentHits(Path file, List<String> hits) throws IOException {
        String name = file.getFileName().toString();
        if (!name.endsWith(".java") || name.contains("ArchitectureGuardTest")) {
            return;
        }
        String source = read(file).toLowerCase(Locale.ROOT);
        for (int index = 0; index < FORBIDDEN_CONTENT_TOKENS.size(); index++) {
            String token = FORBIDDEN_CONTENT_TOKENS.get(index);
            if (source.contains(token)) {
                hits.add(file.getFileName() + " contains " + token);
            }
        }
    }

    private static String joinJava(Path root) throws IOException {
        StringBuilder builder = new StringBuilder();
        List<Path> files = javaFiles(root);
        for (int index = 0; index < files.size(); index++) {
            builder.append(read(files.get(index)));
        }
        return builder.toString();
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        final List<Path> files = new ArrayList<Path>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(".java")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static Path moduleRoot() {
        Path cwd = Paths.get("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("src/main/java/com/aidoctor/diagnosis"))) {
            return cwd;
        }
        Path nested = cwd.resolve("diagnosis-service");
        if (Files.isDirectory(nested.resolve("src/main/java/com/aidoctor/diagnosis"))) {
            return nested;
        }
        throw new IllegalStateException("cannot locate diagnosis-service module from " + cwd);
    }
}
