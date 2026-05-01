package by.gsu.learningplatform.core.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemResponse> handleApiException(final ApiException ex, final HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(problem(
                ex.getType(),
                ex.getStatus().getReasonPhrase(),
                ex.getStatus().value(),
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ProblemResponse> handleValidation(final Exception ex, final HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem(
                "https://learning-platform/errors/validation",
                "Validation failed",
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemResponse> handleAccessDenied(final AccessDeniedException ex, final HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem(
                "https://learning-platform/errors/forbidden",
                "Forbidden",
                HttpStatus.FORBIDDEN.value(),
                "Access is denied",
                request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleUnexpected(final Exception ex, final HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(problem(
                "https://learning-platform/errors/internal",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unexpected server error",
                request.getRequestURI()));
    }

    private ProblemResponse problem(final String type, final String title, final int status, final String detail, final String instance) {
        return new ProblemResponse(type, title, status, detail, instance, MDC.get("correlationId"), OffsetDateTime.now());
    }
}
