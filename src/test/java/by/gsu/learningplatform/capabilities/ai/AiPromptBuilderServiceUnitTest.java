package by.gsu.learningplatform.capabilities.ai;

import lombok.val;
import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptBuilderServiceUnitTest {

    @Test
    void shouldTruncatePromptToConfiguredLimit() {
        val properties = new LearningPlatformProperties(
                new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                new LearningPlatformProperties.PaginationProperties(20, 100),
                new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 60, 21, 20)
        );

        val service = new AiPromptBuilderService(properties);
        val prompt = service.buildAssessmentDraftUserPrompt("x".repeat(200), 6, "medium");

        assertEquals(60, prompt.length());
    }

    @Test
    void shouldContainExpectedSchemaHintsInSystemPrompt() {
        val properties = new LearningPlatformProperties(
                new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                new LearningPlatformProperties.PaginationProperties(20, 100),
                new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 200, 21, 20)
        );
        val service = new AiPromptBuilderService(properties);

        val prompt = service.analyticsSystemPrompt();

        assertTrue(prompt.contains("\"courseSummary\""));
        assertTrue(prompt.contains("\"students\""));
    }

    @Test
    void shouldRequireAssessmentDraftToStayInSourceLanguage() {
        val properties = new LearningPlatformProperties(
                new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                new LearningPlatformProperties.PaginationProperties(20, 100),
                new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 200, 21, 20)
        );
        val service = new AiPromptBuilderService(properties);

        val prompt = service.assessmentDraftSystemPrompt();

        assertTrue(prompt.contains("Use same natural language as the provided course material"));
        assertTrue(prompt.contains("If source material mixes languages, use the dominant language"));
    }

    @Test
    void shouldRequireAnalyticsStringsToStayInSourceLanguage() {
        val properties = new LearningPlatformProperties(
                new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                new LearningPlatformProperties.PaginationProperties(20, 100),
                new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 200, 21, 20)
        );
        val service = new AiPromptBuilderService(properties);

        val prompt = service.analyticsSystemPrompt();

        assertTrue(prompt.contains("Keep JSON field names exactly as shown in schema"));
        assertTrue(prompt.contains("Write all natural-language string values in the same language as the provided course and student data"));
        assertTrue(prompt.contains("If input mixes languages, use the dominant language"));
    }

    @Test
    void shouldRequireStudyPlanStringsToStayInSourceLanguage() {
        val properties = new LearningPlatformProperties(
                new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                new LearningPlatformProperties.PaginationProperties(20, 100),
                new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 200, 21, 20)
        );
        val service = new AiPromptBuilderService(properties);

        val prompt = service.studyPlanSystemPrompt();

        assertTrue(prompt.contains("Keep JSON field names exactly as shown in schema"));
        assertTrue(prompt.contains("Write all natural-language string values in the same language as the provided course and student data"));
        assertTrue(prompt.contains("If input mixes languages, use the dominant language"));
    }
}
