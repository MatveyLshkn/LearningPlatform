package by.gsu.learningplatform.capabilities.lessons;

import java.util.UUID;

public record LessonResponse(UUID id,
                             UUID courseId,
                             String title,
                             String content) {
}
