package by.gsu.learningplatform.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "learning-platform")
public record LearningPlatformProperties(SecurityProperties security,
                                         KeycloakProperties keycloak,
                                         PaginationProperties pagination,
                                         LiquibaseProperties liquibase) {

    public record SecurityProperties(@NotBlank String principalClaim,
                                     @NotBlank String roleClaimPath) {
    }

    public record KeycloakProperties(@NotBlank String adminUrl,
                                     @NotBlank String realm,
                                     @NotBlank String clientId,
                                     @NotBlank String clientSecret) {
    }

    public record PaginationProperties(@Min(1) @Max(100) int defaultLimit,
                                       @Min(1) @Max(500) int maxLimit) {
    }

    public record LiquibaseProperties(@NotBlank String changeLog,
                                      boolean enabled) {
    }
}
