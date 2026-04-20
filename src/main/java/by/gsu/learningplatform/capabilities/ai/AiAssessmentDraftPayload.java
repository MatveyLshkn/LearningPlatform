package by.gsu.learningplatform.capabilities.ai;

import java.util.List;

public record AiAssessmentDraftPayload(String title,
                                       String description,
                                       List<String> questions,
                                       List<String> answerKey,
                                       List<String> rubricCriteria) {
}
