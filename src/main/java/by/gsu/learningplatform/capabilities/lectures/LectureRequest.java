package by.gsu.learningplatform.capabilities.lectures;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LectureRequest(@NotNull UUID lessonId,
                             @NotBlank @Size(max = 255) String title,
                             String videoUrl,
                             String content) {
}
