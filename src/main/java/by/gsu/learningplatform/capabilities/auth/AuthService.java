package by.gsu.learningplatform.capabilities.auth;

import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import by.gsu.learningplatform.core.error.BadRequestException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Map;

@Service
public class AuthService {

    private static final ParameterizedTypeReference<Map<String, Object>> mapType =
            new ParameterizedTypeReference<>() {};
    private static final String grantType = "grant_type";
    private static final String passwordGrant = "password";
    private static final String clientId = "client_id";
    private static final String clientSecret = "client_secret";
    private static final String username = "username";
    private static final String password = "password";
    private static final String accessToken = "access_token";
    private static final String tokenType = "token_type";
    private static final String expiresIn = "expires_in";
    private static final String refreshToken = "refresh_token";
    private static final String refreshExpiresIn = "refresh_expires_in";
    private static final String scope = "scope";

    private final RestClient restClient;
    private final LearningPlatformProperties.KeycloakProperties keycloak;

    public AuthService(LearningPlatformProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
        this.keycloak = properties.keycloak();
    }

    public TokenResponse issueToken(TokenRequest request) {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add(grantType, passwordGrant);
        form.add(clientId, keycloak.clientId());
        form.add(clientSecret, keycloak.clientSecret());
        form.add(username, request.username());
        form.add(password, request.password());

        final Map<String, Object> body;
        try {
            body = restClient.post()
                    .uri(URI.create(keycloak.adminUrl() + "/realms/" + keycloak.realm() + "/protocol/openid-connect/token"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(mapType);
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Invalid username or password");
        }

        if (body == null || body.get(accessToken) == null) {
            throw new BadRequestException("Unable to obtain access token");
        }

        return new TokenResponse(
                String.valueOf(body.get(accessToken)),
                String.valueOf(body.getOrDefault(tokenType, "Bearer")),
                toLong(body.get(expiresIn)),
                asNullableString(body.get(refreshToken)),
                toLong(body.get(refreshExpiresIn)),
                asNullableString(body.get(scope)));
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String asNullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
