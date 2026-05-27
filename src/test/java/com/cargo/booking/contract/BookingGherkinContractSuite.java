package com.cargo.booking.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class BookingGherkinContractSuite {

    private static final String FEATURES_DIR_PROPERTY = "booking.contract.featuresDir";
    private static final String LEGACY_FEATURES_DIR_PROPERTY = "booking.contract.features-dir";

    @Test
    void shouldDiscoverAndValidateBookingGherkinContractSuite() throws IOException {
        Path featureRoot = resolveFeatureRoot();
        List<Path> featureFiles = discoverFeatureFiles(featureRoot);
        List<String> failures = new ArrayList<>();

        if (featureFiles.isEmpty()) {
            failures.add("No .feature files were discovered under " + featureRoot);
        }

        Map<String, Path> featureNames = new HashMap<>();
        int scenarioCount = 0;
        int scenarioOutlineCount = 0;

        for (Path featureFile : featureFiles) {
            FeatureValidation validation = validateFeatureFile(featureRoot, featureFile);
            scenarioCount += validation.scenarioCount();
            scenarioOutlineCount += validation.scenarioOutlineCount();
            failures.addAll(validation.failures());

            if (validation.featureName() != null) {
                Path existing = featureNames.putIfAbsent(validation.featureName(), featureFile);
                if (existing != null) {
                    failures.add("%s duplicates feature name '%s' already used by %s"
                            .formatted(
                                    featureRoot.relativize(featureFile),
                                    validation.featureName(),
                                    featureRoot.relativize(existing)
                            ));
                }
            }
        }

        assertThat(failures)
                .withFailMessage(formatFailure(featureRoot, featureFiles.size(), scenarioCount, failures))
                .isEmpty();

        System.out.printf(
                Locale.ROOT,
                "Booking Gherkin contract suite: discovered %d feature files and %d scenarios (%d outlines) under %s%n",
                featureFiles.size(),
                scenarioCount,
                scenarioOutlineCount,
                featureRoot
        );
    }

    private static Path resolveFeatureRoot() {
        String configuredRoot = System.getProperty(
                FEATURES_DIR_PROPERTY,
                System.getProperty(LEGACY_FEATURES_DIR_PROPERTY, "test/features")
        );
        Path root = Path.of(configuredRoot);
        if (!root.isAbsolute()) {
            root = Path.of(System.getProperty("user.dir")).resolve(root);
        }
        return root.normalize();
    }

    private static List<Path> discoverFeatureFiles(Path featureRoot) throws IOException {
        if (!Files.isDirectory(featureRoot)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(featureRoot)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".feature"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static FeatureValidation validateFeatureFile(Path featureRoot, Path featureFile) throws IOException {
        List<String> lines = Files.readAllLines(featureFile);
        List<String> failures = new ArrayList<>();
        int featureCount = 0;
        int scenarioCount = 0;
        int scenarioOutlineCount = 0;
        boolean hasContractTag = false;
        boolean hasBlackBoxTag = false;
        String featureName = null;

        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String trimmed = lines.get(index).trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("@")) {
                List<String> tags = Stream.of(trimmed.split("\\s+"))
                        .filter(tag -> tag.startsWith("@"))
                        .toList();
                hasContractTag = hasContractTag || tags.contains("@contract");
                hasBlackBoxTag = hasBlackBoxTag || tags.contains("@black-box");
                if (tags.contains("@wip")) {
                    failures.add(location(featureRoot, featureFile, lineNumber)
                            + " uses @wip, which is excluded from CI contract execution");
                }
                continue;
            }

            if (trimmed.startsWith("Feature:")) {
                featureCount++;
                featureName = trimmed.substring("Feature:".length()).trim();
                if (featureName.isBlank()) {
                    failures.add(location(featureRoot, featureFile, lineNumber) + " has an empty Feature name");
                }
                continue;
            }

            if (trimmed.startsWith("Scenario Outline:")) {
                scenarioCount++;
                scenarioOutlineCount++;
                if (trimmed.substring("Scenario Outline:".length()).trim().isBlank()) {
                    failures.add(location(featureRoot, featureFile, lineNumber) + " has an empty Scenario Outline name");
                }
                continue;
            }

            if (trimmed.startsWith("Scenario:")) {
                scenarioCount++;
                if (trimmed.substring("Scenario:".length()).trim().isBlank()) {
                    failures.add(location(featureRoot, featureFile, lineNumber) + " has an empty Scenario name");
                }
            }
        }

        if (featureCount != 1) {
            failures.add("%s must contain exactly one Feature declaration, found %d"
                    .formatted(featureRoot.relativize(featureFile), featureCount));
        }
        if (scenarioCount == 0) {
            failures.add(featureRoot.relativize(featureFile) + " must contain at least one Scenario or Scenario Outline");
        }
        if (!hasContractTag) {
            failures.add(featureRoot.relativize(featureFile) + " must include the @contract tag");
        }
        if (!hasBlackBoxTag) {
            failures.add(featureRoot.relativize(featureFile) + " must include the @black-box tag");
        }

        return new FeatureValidation(featureName, scenarioCount, scenarioOutlineCount, failures);
    }

    private static String location(Path featureRoot, Path featureFile, int lineNumber) {
        return featureRoot.relativize(featureFile) + ":" + lineNumber;
    }

    private static String formatFailure(Path featureRoot, int featureFileCount, int scenarioCount, List<String> failures) {
        String newline = System.lineSeparator();
        StringBuilder message = new StringBuilder()
                .append("Booking Gherkin contract suite validation failed").append(newline)
                .append("Feature root: ").append(featureRoot).append(newline)
                .append("Discovered feature files: ").append(featureFileCount).append(newline)
                .append("Discovered scenarios: ").append(scenarioCount).append(newline)
                .append("Failures:").append(newline);
        failures.stream()
                .filter(Objects::nonNull)
                .forEach(failure -> message.append(" - ").append(failure).append(newline));
        return message.toString();
    }

    private record FeatureValidation(
            String featureName,
            int scenarioCount,
            int scenarioOutlineCount,
            List<String> failures
    ) {
    }
}
