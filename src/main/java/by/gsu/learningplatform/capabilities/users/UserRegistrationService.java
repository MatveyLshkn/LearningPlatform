package by.gsu.learningplatform.capabilities.users;

import lombok.val;
import by.gsu.learningplatform.core.error.BadRequestException;
import by.gsu.learningplatform.core.error.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    public UserRegistrationService(final UserRepository userRepository, final KeycloakAdminClient keycloakAdminClient) {
        this.userRepository = userRepository;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    @Transactional
    public UserRegistrationResponse register(final UserRegistrationRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new BadRequestException("ADMIN cannot be self-assigned");
        }

        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new ConflictException("Username already exists");
        });
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            throw new ConflictException("Email already exists");
        });

        val keycloakSub = keycloakAdminClient.createUserAndAssignRole(
                request.username(),
                request.email(),
                request.password(),
                request.role().name());

        val entity = new UserEntity();
        entity.setKeycloakSub(keycloakSub);
        entity.setUsername(request.username());
        entity.setEmail(request.email());
        entity.setRole(request.role());

        val saved = userRepository.save(entity);

        return new UserRegistrationResponse(
                saved.getId(),
                saved.getKeycloakSub(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole().name());
    }
}
