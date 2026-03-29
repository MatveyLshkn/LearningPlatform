package by.gsu.learningplatform.capabilities.assessments;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssessmentResponse(UUID id,
                                 UUID courseId,
                                 UUID createdBy,
                                 OffsetDateTime createdAt) {
}
