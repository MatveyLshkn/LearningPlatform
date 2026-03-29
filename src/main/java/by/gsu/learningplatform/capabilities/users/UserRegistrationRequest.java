package by.gsu.learningplatform.capabilities.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(@NotBlank @Size(min = 3, max = 64) String username,
                                      @Email @NotBlank String email,
                                      @NotBlank @Size(min = 8, max = 100) String password,
                                      @NotNull UserRole role) {
}
