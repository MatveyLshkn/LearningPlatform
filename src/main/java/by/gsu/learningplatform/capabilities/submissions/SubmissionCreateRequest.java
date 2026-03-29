package by.gsu.learningplatform.capabilities.submissions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmissionCreateRequest(@NotNull UUID assessmentId,
                                      @NotBlank String answerText) {
}
