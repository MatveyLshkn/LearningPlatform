package by.gsu.learningplatform.capabilities.courses;

import java.util.UUID;

public record CourseCatalogLectureResponse(UUID id,
                                           UUID lessonId,
                                           String title) {
}
