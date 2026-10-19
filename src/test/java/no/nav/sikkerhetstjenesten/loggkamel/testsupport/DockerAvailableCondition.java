package no.nav.sikkerhetstjenesten.loggkamel.testsupport;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

/**
 * Skips tests that need a Testcontainers database when Docker is unavailable, so that a plain
 * `mvn test` still passes on machines without Docker. The pipeline has Docker and runs them.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static final Logger log = LoggerFactory.getLogger(DockerAvailableCondition.class);

    @Override
    public @NonNull ConditionEvaluationResult evaluateExecutionCondition(@NonNull ExtensionContext context) {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            return ConditionEvaluationResult.enabled("Docker is available");
        }
        if (context.getTestMethod().isEmpty()) {
            log.warn("Docker is not available, skipping {}", context.getRequiredTestClass().getSimpleName());
        }
        return ConditionEvaluationResult.disabled("Docker is not available");
    }
}
