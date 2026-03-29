package by.gsu.learningplatform.capabilities.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String username,
                           @NotBlank String password) {
}
