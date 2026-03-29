package by.gsu.learningplatform.core.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RoleClaimConverter implements Converter<Map<String, Object>, Collection<GrantedAuthority>> {

    private static final String realmAccessClaim = "realm_access";
    private static final String rolesClaim = "roles";
    private static final String rolePrefix = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Map<String, Object> source) {
        final var realmAccess = source.get(realmAccessClaim);
        if (!(realmAccess instanceof Map<?, ?> realmMap)) {
            return List.of();
        }

        final var roles = realmMap.get(rolesClaim);
        if (!(roles instanceof List<?> roleList)) {
            return List.of();
        }

        return roleList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith(rolePrefix) ? role : rolePrefix + role.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    public static Set<String> toRoleNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
