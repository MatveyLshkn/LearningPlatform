package by.gsu.learningplatform.capabilities.auth;

public record TokenResponse(String accessToken,
                            String tokenType,
                            long expiresIn,
                            String refreshToken,
                            long refreshExpiresIn,
                            String scope) {
}
