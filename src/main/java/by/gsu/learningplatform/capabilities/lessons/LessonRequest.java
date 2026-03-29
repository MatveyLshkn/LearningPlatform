package by.gsu.learningplatform.capabilities.lessons;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LessonRequest(@NotNull UUID courseId,
                            @NotBlank @Size(max = 255) String title,
                            @NotBlank String content) {
}
