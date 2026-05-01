package by.gsu.learningplatform.core.error;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(final String message) {
        super(HttpStatus.FORBIDDEN, "https://learning-platform/errors/forbidden", message);
    }
}
