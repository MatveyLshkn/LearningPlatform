package by.gsu.learningplatform.core.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemResponse(String type,
                              String title,
                              int status,
                              String detail,
                              String instance,
                              String correlationId,
                              OffsetDateTime timestamp) {
}
