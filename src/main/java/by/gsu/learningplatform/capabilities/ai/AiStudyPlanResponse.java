package by.gsu.learningplatform.capabilities.ai;

import java.util.List;
import java.util.UUID;

public record AiStudyPlanResponse(UUID courseId,
                                  UUID studentId,
                                  List<String> prioritizedGoals,
                                  List<String> weeklyTargets,
                                  List<String> recommendedLessons,
                                  List<String> recommendedLectures,
                                  String rationale) {
}
