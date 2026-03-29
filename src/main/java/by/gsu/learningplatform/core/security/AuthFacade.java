package by.gsu.learningplatform.core.security;

import by.gsu.learningplatform.core.error.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthFacade {

    private static final String preferredUsernameClaim = "preferred_username";
    private static final String userIdClaim = "user_id";

    public AppPrincipal currentPrincipal() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            throw new ForbiddenException("Unauthenticated access");
        }

        final var jwt = authentication.getToken();
        final var sub = jwt.getSubject();
        final var username = jwt.getClaimAsString(preferredUsernameClaim);
        final var userIdClaimValue = jwt.getClaimAsString(userIdClaim);
        final var userId = userIdClaimValue == null ? null : UUID.fromString(userIdClaimValue);

        return new AppPrincipal(userId, sub, username, RoleClaimConverter.toRoleNames(authentication.getAuthorities()));
    }
}
