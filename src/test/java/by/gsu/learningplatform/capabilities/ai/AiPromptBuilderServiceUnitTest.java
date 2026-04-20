package by.gsu.learningplatform.capabilities.ai;

import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptBuilderServiceUnitTest {

    @Test
    void shouldTruncatePromptToConfiguredLimit() {
        final var properties = new LearningPlatformProperties(
                new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                new LearningPlatformProperties.PaginationProperties(20, 100),
                new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 60, 21, 20)
        );

        final var service = new AiPromptBuilderService(properties);
        final var prompt = service.buildAssessmentDraftUserPrompt("x".repeat(200), 6, "medium");

        assertEquals(60, prompt.length());
    }

    @Test
    void shouldContainExpectedSchemaHintsInSystemPrompt() {
        final var properties = new LearningPlatformProperties(
                new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                new LearningPlatformProperties.PaginationProperties(20, 100),
                new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 200, 21, 20)
        );
        final var service = new AiPromptBuilderService(properties);

        final var prompt = service.analyticsSystemPrompt();

        assertTrue(prompt.contains("\"courseSummary\""));
        assertTrue(prompt.contains("\"students\""));
    }
}
