package by.gsu.learningplatform.capabilities.courses;

import java.util.List;
import java.util.UUID;

public record CourseCatalogLessonResponse(UUID id,
                                          UUID courseId,
                                          String title,
                                          List<CourseCatalogLectureResponse> lectures) {
}
