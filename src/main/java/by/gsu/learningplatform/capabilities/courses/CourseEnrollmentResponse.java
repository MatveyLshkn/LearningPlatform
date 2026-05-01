package by.gsu.learningplatform.capabilities.courses;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CourseEnrollmentResponse(UUID enrollmentId,
                                       OffsetDateTime enrolledAt,
                                       CourseResponse course) {
}
