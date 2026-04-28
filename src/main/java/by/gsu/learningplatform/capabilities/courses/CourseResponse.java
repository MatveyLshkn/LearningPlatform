package by.gsu.learningplatform.capabilities.courses;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CourseResponse(UUID id,
                             String title,
                             String description,
                             UUID teacherId,
                             OffsetDateTime createdAt,
                             List<String> tags) {
}
