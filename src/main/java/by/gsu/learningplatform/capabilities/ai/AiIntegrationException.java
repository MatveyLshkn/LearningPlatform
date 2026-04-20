package by.gsu.learningplatform.capabilities.ai;

import by.gsu.learningplatform.core.error.ApiException;
import org.springframework.http.HttpStatus;

public class AiIntegrationException extends ApiException {

    public AiIntegrationException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_INTEGRATION_ERROR", message);
    }
}
