package by.gsu.learningplatform.capabilities.assessments;

import java.util.List;
import java.util.UUID;

public record AssessmentDraftResponse(UUID courseId,
                                      UUID lessonId,
                                      UUID lectureId,
                                      String title,
                                      String description,
                                      List<String> questions,
                                      List<String> answerKey,
                                      List<String> rubricCriteria) {
}
