package by.gsu.learningplatform.capabilities.assessments;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssessmentGenerateRequest(@NotNull UUID courseId,
                                        UUID lessonId,
                                        UUID lectureId,
                                        @Min(3) @Max(20) Integer questionCount,
                                        @Size(max = 32) String difficulty) {
}
