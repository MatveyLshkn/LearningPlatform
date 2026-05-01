package by.gsu.learningplatform.capabilities.assessments;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssessmentStudentResponse(UUID id,
                                        UUID courseId,
                                        String title,
                                        String description,
                                        List<String> questions,
                                        String sourceType,
                                        UUID sourceId,
                                        OffsetDateTime createdAt) {
}
