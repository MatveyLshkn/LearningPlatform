package by.gsu.learningplatform.capabilities.courses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CourseRequest(@NotBlank @Size(max = 255) String title,
                            @Size(max = 5000) String description,
                            @Size(max = 20) List<@NotBlank @Size(max = 64) String> tags) {
}
