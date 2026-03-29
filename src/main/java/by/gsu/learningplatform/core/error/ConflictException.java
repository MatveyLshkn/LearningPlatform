package by.gsu.learningplatform.core.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "https://learning-platform/errors/conflict", message);
    }
}
