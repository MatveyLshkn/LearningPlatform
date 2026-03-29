package by.gsu.learningplatform.capabilities.assessments;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssessmentCreateRequest(@NotNull UUID courseId) {
}
