package by.gsu.learningplatform.capabilities.users;

import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import by.gsu.learningplatform.core.error.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakAdminClient {

    private final RestClient restClient;
    private final LearningPlatformProperties.KeycloakProperties keycloak;
    private static final ParameterizedTypeReference<Map<String, Object>> mapType =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> listOfMapsType =
            new ParameterizedTypeReference<>() {};
    private static final String authorizationHeader = "Authorization";
    private static final String bearerPrefix = "Bearer ";
    private static final String locationHeader = "Location";
    private static final String accessTokenField = "access_token";
    private static final String passwordCredentialType = "password";

    public KeycloakAdminClient(LearningPlatformProperties properties, RestClient.Builder restClientBuilder) {
        this.keycloak = properties.keycloak();
        this.restClient = restClientBuilder.build();
    }

    public String createUserAndAssignRole(String username, String email, String password, String role) {
        final var adminToken = getAdminAccessToken();

        final var userPayload = Map.of(
                "username", username,
                "email", email,
                "enabled", true,
                "firstName", username,
                "lastName", "User",
                "emailVerified", true
        );

        final var createResponse = restClient.post()
                .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/users"))
                .header(authorizationHeader, bearerPrefix + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userPayload)
                .retrieve()
                .toBodilessEntity();

        final var location = createResponse.getHeaders().getFirst(locationHeader);
        if (location == null || location.isBlank()) {
            throw new BadRequestException("Unable to obtain Keycloak user identifier");
        }
        final var userId = location.substring(location.lastIndexOf('/') + 1);

        setPassword(adminToken, userId, password);
        assignRealmRole(adminToken, userId, role);
        return userId;
    }

    private void setPassword(String adminToken, String userId, String password) {
        final var credentialPayload = Map.of(
                "type", passwordCredentialType,
                "value", password,
                "temporary", false
        );

        restClient.put()
                .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/users/" + userId + "/reset-password"))
                .header(authorizationHeader, bearerPrefix + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentialPayload)
                .retrieve()
                .toBodilessEntity();
    }

    private String getAdminAccessToken() {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloak.clientId());
        form.add("client_secret", keycloak.clientSecret());

        final var body = restClient.post()
                .uri(URI.create(keycloak.adminUrl() + "/realms/" + keycloak.realm() + "/protocol/openid-connect/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(mapType);

        if (body == null || !body.containsKey(accessTokenField)) {
            throw new BadRequestException("Unable to get Keycloak admin token");
        }
        return String.valueOf(body.get(accessTokenField));
    }

    private void assignRealmRole(String adminToken, String userId, String roleName) {
        final var allRoles = restClient.get()
                .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/roles"))
                .header(authorizationHeader, bearerPrefix + adminToken)
                .retrieve()
                .body(listOfMapsType);

        if (allRoles == null) {
            throw new BadRequestException("Unable to load Keycloak roles");
        }

        final var role = allRoles.stream()
                .filter(r -> roleName.equalsIgnoreCase(String.valueOf(r.get("name"))))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Role not found in Keycloak: " + roleName));

        restClient.post()
                .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/users/" + userId + "/role-mappings/realm"))
                .header(authorizationHeader, bearerPrefix + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }
}
