package by.gsu.learningplatform.capabilities.users;

import by.gsu.learningplatform.core.error.BadRequestException;
import by.gsu.learningplatform.core.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private UserRegistrationService service;

    @Test
    void shouldRejectAdminSelfRegistration() {
        final var request = new UserRegistrationRequest("admin", "admin@example.com", "Passw0rd!", UserRole.ADMIN);

        assertThrows(BadRequestException.class, () -> service.register(request));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        final var request = new UserRegistrationRequest("student", "student@example.com", "Passw0rd!", UserRole.STUDENT);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(new UserEntity()));

        assertThrows(ConflictException.class, () -> service.register(request));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        final var request = new UserRegistrationRequest("student", "student@example.com", "Passw0rd!", UserRole.STUDENT);
        when(userRepository.findByUsername("student")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(new UserEntity()));

        assertThrows(ConflictException.class, () -> service.register(request));
    }

    @Test
    void shouldRegisterStudent() {
        final var request = new UserRegistrationRequest("student", "student@example.com", "Passw0rd!", UserRole.STUDENT);
        when(userRepository.findByUsername("student")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(keycloakAdminClient.createUserAndAssignRole("student", "student@example.com", "Passw0rd!", "STUDENT"))
                .thenReturn("kc-sub-1");

        final var saved = new UserEntity();
        saved.setId(UUID.randomUUID());
        saved.setKeycloakSub("kc-sub-1");
        saved.setUsername("student");
        saved.setEmail("student@example.com");
        saved.setRole(UserRole.STUDENT);

        when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

        final var response = service.register(request);

        assertEquals("kc-sub-1", response.keycloakSub());
        assertEquals("student", response.username());
        assertEquals("STUDENT", response.role());
    }
}
