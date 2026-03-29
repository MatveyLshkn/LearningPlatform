package by.gsu.learningplatform.capabilities.courses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(@NotBlank @Size(max = 255) String title,
                            @Size(max = 5000) String description) {
}
