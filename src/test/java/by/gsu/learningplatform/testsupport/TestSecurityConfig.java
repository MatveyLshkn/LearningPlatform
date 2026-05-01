package by.gsu.learningplatform.testsupport;

import lombok.val;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    private static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_ROLES = "roles";
    private static final String ADMIN_TOKEN = "admin-token";
    private static final String TEACHER_TOKEN = "teacher-token";
    private static final String OTHER_TEACHER_TOKEN = "other-teacher-token";
    private static final String STUDENT_TOKEN = "student-token";

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            val profile = profile(token);
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .claim("sub", profile.sub())
                    .claim(CLAIM_PREFERRED_USERNAME, profile.username())
                    .claim(CLAIM_REALM_ACCESS, Map.of(CLAIM_ROLES, profile.roles()))
                    .build();
        };
    }

    private TokenProfile profile(final String token) {
        return switch (token) {
            case ADMIN_TOKEN -> new TokenProfile("admin-sub", "admin-user", List.of("ADMIN"));
            case TEACHER_TOKEN -> new TokenProfile("teacher-sub", "teacher-user", List.of("TEACHER"));
            case OTHER_TEACHER_TOKEN -> new TokenProfile("other-teacher-sub", "other-teacher", List.of("TEACHER"));
            case STUDENT_TOKEN -> new TokenProfile("student-sub", "student-user", List.of("STUDENT"));
            default -> new TokenProfile("unknown-sub", "unknown-user", List.of());
        };
    }

    private record TokenProfile(String sub, String username, List<String> roles) {
    }
}
