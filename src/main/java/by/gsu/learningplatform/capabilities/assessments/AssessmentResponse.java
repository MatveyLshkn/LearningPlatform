package by.gsu.learningplatform.capabilities.assessments;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssessmentResponse(UUID id,
                                 UUID courseId,
                                 UUID createdBy,
                                 String title,
                                 String description,
                                 String questionsJson,
                                 String answerKeyJson,
                                 String rubricJson,
                                 String sourceType,
                                 UUID sourceId,
                                 OffsetDateTime createdAt) {
}
