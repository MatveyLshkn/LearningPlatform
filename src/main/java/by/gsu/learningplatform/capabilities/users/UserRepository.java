package by.gsu.learningplatform.capabilities.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByKeycloakSub(String keycloakSub);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    long countByRole(by.gsu.learningplatform.capabilities.users.UserRole role);
}
