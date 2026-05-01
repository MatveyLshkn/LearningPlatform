package by.gsu.learningplatform.capabilities.courses;

import java.util.List;

public record CourseCatalogDetailsResponse(CourseResponse course,
                                           List<CourseCatalogLessonResponse> lessons) {
}
