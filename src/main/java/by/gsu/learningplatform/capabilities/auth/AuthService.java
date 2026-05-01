package by.gsu.learningplatform.capabilities.auth;

import lombok.val;
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

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final String GRANT_TYPE = "grant_type";
    private static final String PASSWORD_GRANT = "password";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String TOKEN_TYPE = "token_type";
    private static final String EXPIRES_IN = "expires_in";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String REFRESH_EXPIRES_IN = "refresh_expires_in";
    private static final String SCOPE = "scope";

    private final RestClient restClient;
    private final LearningPlatformProperties.KeycloakProperties keycloak;

    public AuthService(final LearningPlatformProperties properties, final RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
        this.keycloak = properties.keycloak();
    }

    public TokenResponse issueToken(final TokenRequest request) {
        val form = new LinkedMultiValueMap<String, String>();
        form.add(GRANT_TYPE, PASSWORD_GRANT);
        form.add(CLIENT_ID, keycloak.clientId());
        form.add(CLIENT_SECRET, keycloak.clientSecret());
        form.add(USERNAME, request.username());
        form.add(PASSWORD, request.password());

        final Map<String, Object> body;
        try {
            body = restClient.post()
                    .uri(URI.create(keycloak.adminUrl() + "/realms/" + keycloak.realm() + "/protocol/openid-connect/token"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Invalid username or password");
        }

        if (body == null || body.get(ACCESS_TOKEN) == null) {
            throw new BadRequestException("Unable to obtain access token");
        }

        return new TokenResponse(
                String.valueOf(body.get(ACCESS_TOKEN)),
                String.valueOf(body.getOrDefault(TOKEN_TYPE, "Bearer")),
                toLong(body.get(EXPIRES_IN)),
                asNullableString(body.get(REFRESH_TOKEN)),
                toLong(body.get(REFRESH_EXPIRES_IN)),
                asNullableString(body.get(SCOPE)));
    }

    private long toLong(final Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String asNullableString(final Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
