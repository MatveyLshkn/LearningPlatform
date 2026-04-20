package by.gsu.learningplatform.capabilities.assessments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AssessmentCreateFromDraftRequest(@NotNull UUID courseId,
                                               UUID lessonId,
                                               UUID lectureId,
                                               @NotBlank @Size(max = 255) String title,
                                               @NotBlank @Size(max = 5000) String description,
                                               @NotEmpty List<@NotBlank String> questions,
                                               @NotEmpty List<@NotBlank String> answerKey,
                                               @NotEmpty List<@NotBlank String> rubricCriteria) {
}
