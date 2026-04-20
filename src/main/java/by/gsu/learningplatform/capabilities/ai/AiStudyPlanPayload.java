package by.gsu.learningplatform.capabilities.ai;

import java.util.List;

public record AiStudyPlanPayload(List<String> prioritizedGoals,
                                 List<String> weeklyTargets,
                                 List<String> lessonOrder,
                                 List<String> lectureOrder,
                                 String rationale) {
}
