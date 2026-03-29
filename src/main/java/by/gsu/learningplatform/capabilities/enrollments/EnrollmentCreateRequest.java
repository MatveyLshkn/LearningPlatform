package by.gsu.learningplatform.capabilities.enrollments;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnrollmentCreateRequest(@NotNull UUID courseId) {
}
