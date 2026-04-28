package by.gsu.learningplatform.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    private static final String claimPreferredUsername = "preferred_username";
    private static final String claimRealmAccess = "realm_access";
    private static final String claimRoles = "roles";
    private static final String adminToken = "admin-token";
    private static final String teacherToken = "teacher-token";
    private static final String otherTeacherToken = "other-teacher-token";
    private static final String studentToken = "student-token";

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            final var profile = profile(token);
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .claim("sub", profile.sub())
                    .claim(claimPreferredUsername, profile.username())
                    .claim(claimRealmAccess, Map.of(claimRoles, profile.roles()))
                    .build();
        };
    }

    private TokenProfile profile(String token) {
        return switch (token) {
            case adminToken -> new TokenProfile("admin-sub", "admin-user", List.of("ADMIN"));
            case teacherToken -> new TokenProfile("teacher-sub", "teacher-user", List.of("TEACHER"));
            case otherTeacherToken -> new TokenProfile("other-teacher-sub", "other-teacher", List.of("TEACHER"));
            case studentToken -> new TokenProfile("student-sub", "student-user", List.of("STUDENT"));
            default -> new TokenProfile("unknown-sub", "unknown-user", List.of());
        };
    }

    private record TokenProfile(String sub, String username, List<String> roles) {
    }
}
