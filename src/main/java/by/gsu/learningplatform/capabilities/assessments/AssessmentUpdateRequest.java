package by.gsu.learningplatform.capabilities.assessments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssessmentUpdateRequest(@Size(max = 255) String title,
                                      @Size(max = 5000) String description,
                                      List<@NotBlank String> questions,
                                      List<@NotBlank String> answerKey,
                                      List<@NotBlank String> rubricCriteria) {
}
