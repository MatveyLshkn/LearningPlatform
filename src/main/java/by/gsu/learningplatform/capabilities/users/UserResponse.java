package by.gsu.learningplatform.capabilities.users;


import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(UUID id,
                           String username,
                           String email,
                           UserRole role,
                           OffsetDateTime createdAt) {
}
