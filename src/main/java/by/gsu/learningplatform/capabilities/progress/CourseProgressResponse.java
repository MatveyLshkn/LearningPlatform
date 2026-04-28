package by.gsu.learningplatform.capabilities.progress;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CourseProgressResponse(UUID courseId,
                                     UUID userId,
                                     long totalLectures,
                                     long completedLectures,
                                     double progressPercent,
                                     List<UUID> completedLectureIds,
                                     OffsetDateTime lastCompletedAt) {
}
