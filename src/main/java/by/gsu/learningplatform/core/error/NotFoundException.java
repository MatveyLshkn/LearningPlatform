package by.gsu.learningplatform.core.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "https://learning-platform/errors/not-found", message);
    }
}
