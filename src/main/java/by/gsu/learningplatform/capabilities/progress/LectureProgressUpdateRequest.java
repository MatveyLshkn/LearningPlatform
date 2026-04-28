package by.gsu.learningplatform.capabilities.progress;

import jakarta.validation.constraints.NotNull;

public record LectureProgressUpdateRequest(@NotNull Boolean completed) {
}
