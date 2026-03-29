package by.gsu.learningplatform.capabilities.lectures;

import java.util.UUID;

public record LectureResponse(UUID id,
                              UUID lessonId,
                              String title,
                              String videoUrl,
                              String content) {
}
