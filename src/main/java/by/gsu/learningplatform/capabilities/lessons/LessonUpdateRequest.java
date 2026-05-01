package by.gsu.learningplatform.capabilities.lessons;

import jakarta.validation.constraints.Size;

public record LessonUpdateRequest(@Size(max = 255) String title,
                                  String content) {
}
