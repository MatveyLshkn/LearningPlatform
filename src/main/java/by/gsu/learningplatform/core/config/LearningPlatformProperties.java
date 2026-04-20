package by.gsu.learningplatform.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "learning-platform")
public record LearningPlatformProperties(SecurityProperties security,
                                         KeycloakProperties keycloak,
                                         PaginationProperties pagination,
                                         LiquibaseProperties liquibase,
                                         AiProperties ai) {

    public record SecurityProperties(@NotBlank String principalClaim,
                                     @NotBlank String roleClaimPath) {
    }

    public record KeycloakProperties(@NotBlank String adminUrl,
                                     @NotBlank String realm,
                                     @NotBlank String clientId,
                                     @NotBlank String clientSecret,
                                     @NotBlank String adminUsername,
                                     @NotBlank String adminPassword) {
    }

    public record PaginationProperties(@Min(1) @Max(100) int defaultLimit,
                                       @Min(1) @Max(500) int maxLimit) {
    }

    public record LiquibaseProperties(@NotBlank String changeLog,
                                      boolean enabled) {
    }

    public record AiProperties(@NotBlank String model,
                               @NotNull Double temperature,
                               @Min(1) @Max(4096) int maxTokens,
                               @Positive int maxPromptChars,
                               @Min(1) @Max(120) int analyticsRecentDays,
                               @Min(1) @Max(120) int timeoutSeconds) {
    }
}
