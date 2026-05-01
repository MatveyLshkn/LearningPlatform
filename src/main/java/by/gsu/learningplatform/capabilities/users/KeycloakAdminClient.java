package by.gsu.learningplatform.capabilities.users;

import lombok.val;
import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import by.gsu.learningplatform.core.error.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientResponseException;
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
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String LOCATION_HEADER = "Location";
    private static final String ACCESS_TOKEN_FIELD = "access_token";
    private static final String PASSWORD_CREDENTIAL_TYPE = "password";
    private static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
    private static final String MASTER_REALM = "master";

    public KeycloakAdminClient(final LearningPlatformProperties properties, final RestClient.Builder restClientBuilder) {
        this.keycloak = properties.keycloak();
        this.restClient = restClientBuilder.build();
    }

    public String createUserAndAssignRole(final String username, final String email, final String password, final String role) {
        val adminToken = getAdminAccessToken();

        val userPayload = Map.of(
                "username", username,
                "email", email,
                "enabled", true,
                "firstName", username,
                "lastName", "User",
                "emailVerified", true
        );

        val createResponse = executeKeycloakRequest(() -> restClient.post()
                .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/users"))
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userPayload)
                .retrieve()
                .toBodilessEntity());

        val location = createResponse.getHeaders().getFirst(LOCATION_HEADER);
        if (location == null || location.isBlank()) {
            throw new BadRequestException("Unable to obtain Keycloak user identifier");
        }
        val userId = location.substring(location.lastIndexOf('/') + 1);

        setPassword(adminToken, userId, password);
        assignRealmRole(adminToken, userId, role);
        return userId;
    }

    private void setPassword(final String adminToken, final String userId, final String password) {
        val credentialPayload = Map.of(
                "type", PASSWORD_CREDENTIAL_TYPE,
                "value", password,
                "temporary", false
        );

        executeKeycloakRequest(() -> restClient.put()
                .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/users/" + userId + "/reset-password"))
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentialPayload)
                .retrieve()
                .toBodilessEntity());
    }

    private String getAdminAccessToken() {
        try {
            val token = getServiceAccountToken();
            if (canAccessRealmAdministration(token)) {
                return token;
            }
            return getMasterAdminPasswordToken();
        } catch (RestClientResponseException ex) {
            return getMasterAdminPasswordToken();
        }
    }

    private String getServiceAccountToken() {
        val form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloak.clientId());
        form.add("client_secret", keycloak.clientSecret());
        val body = executeKeycloakRequest(() -> restClient.post()
                .uri(URI.create(keycloak.adminUrl() + "/realms/" + keycloak.realm() + "/protocol/openid-connect/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(mapType));
        return extractAccessToken(body);
    }

    private String getMasterAdminPasswordToken() {
        val form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", ADMIN_CLI_CLIENT_ID);
        form.add("username", keycloak.adminUsername());
        form.add("password", keycloak.adminPassword());
        val body = executeKeycloakRequest(() -> restClient.post()
                .uri(URI.create(keycloak.adminUrl() + "/realms/" + MASTER_REALM + "/protocol/openid-connect/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(mapType));
        return extractAccessToken(body);
    }

    private String extractAccessToken(final Map<String, Object> body) {
        if (body == null || !body.containsKey(ACCESS_TOKEN_FIELD)) {
            throw new BadRequestException("Unable to get Keycloak admin token");
        }
        return String.valueOf(body.get(ACCESS_TOKEN_FIELD));
    }

    private boolean canAccessRealmAdministration(final String adminToken) {
        try {
            restClient.get()
                    .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/roles"))
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            return false;
        }
    }

    private void assignRealmRole(final String adminToken, final String userId, final String roleName) {
        val allRoles = executeKeycloakRequest(() -> restClient.get()
                .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/roles"))
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .retrieve()
                .body(listOfMapsType));

        if (allRoles == null) {
            throw new BadRequestException("Unable to load Keycloak roles");
        }

        val role = allRoles.stream()
                .filter(r -> roleName.equalsIgnoreCase(String.valueOf(r.get("name"))))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Role not found in Keycloak: " + roleName));

        executeKeycloakRequest(() -> restClient.post()
                .uri(URI.create(keycloak.adminUrl() + "/admin/realms/" + keycloak.realm() + "/users/" + userId + "/role-mappings/realm"))
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity());
    }

    private <T> T executeKeycloakRequest(final KeycloakCall<T> action) {
        try {
            return action.call();
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("Keycloak request failed: " + ex.getStatusCode().value());
        } catch (Exception ex) {
            throw new BadRequestException("Keycloak request failed");
        }
    }

    @FunctionalInterface
    private interface KeycloakCall<T> {
        T call();
    }
}
