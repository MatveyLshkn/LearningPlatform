package by.gsu.learningplatform.capabilities.users;

import by.gsu.learningplatform.core.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserEntity getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public UserEntity getByKeycloakSub(String keycloakSub) {
        return userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new NotFoundException("User not found for keycloak subject"));
    }

    public UserResponse getUserResponse(UUID id) {
        return userMapper.toResponse(getById(id));
    }
}
