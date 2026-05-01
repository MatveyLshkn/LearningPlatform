package by.gsu.learningplatform.core.security;

import lombok.val;
import by.gsu.learningplatform.core.error.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthFacade {

    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String USER_ID_CLAIM = "user_id";

    public AppPrincipal currentPrincipal() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            throw new ForbiddenException("Unauthenticated access");
        }

        val jwt = authentication.getToken();
        val sub = jwt.getSubject();
        val username = jwt.getClaimAsString(PREFERRED_USERNAME_CLAIM);
        val USER_ID_CLAIMValue = jwt.getClaimAsString(USER_ID_CLAIM);
        val userId = USER_ID_CLAIMValue == null ? null : UUID.fromString(USER_ID_CLAIMValue);

        return new AppPrincipal(userId, sub, username, RoleClaimConverter.toRoleNames(authentication.getAuthorities()));
    }
}
