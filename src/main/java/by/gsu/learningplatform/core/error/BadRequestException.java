package by.gsu.learningplatform.core.error;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(final String message) {
        super(HttpStatus.BAD_REQUEST, "https://learning-platform/errors/bad-request", message);
    }
}
