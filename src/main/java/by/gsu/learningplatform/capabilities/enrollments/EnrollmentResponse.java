package by.gsu.learningplatform.capabilities.enrollments;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EnrollmentResponse(UUID id,
                                 UUID userId,
                                 UUID courseId,
                                 OffsetDateTime enrolledAt) {
}
