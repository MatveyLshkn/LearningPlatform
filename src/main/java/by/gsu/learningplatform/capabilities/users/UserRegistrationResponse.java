package by.gsu.learningplatform.capabilities.users;

import java.util.UUID;

public record UserRegistrationResponse(UUID userId,
                                       String keycloakSub,
                                       String username,
                                       String email,
                                       String role) {
}
