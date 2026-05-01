package by.gsu.learningplatform.core.security;

import java.util.Set;
import java.util.UUID;

public record AppPrincipal(UUID userId,
                           String keycloakSub,
                           String username,
                           Set<String> roles) {

    private static final String ROLE_PREFIX = "ROLE_";

    public boolean hasRole(final String role) {
        return roles.contains(role) || roles.contains(ROLE_PREFIX + role);
    }
}
