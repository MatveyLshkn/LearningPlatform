package by.gsu.learningplatform.capabilities.ai;

import java.util.List;

public record AiAnalyticsPayload(String courseSummary,
                                 List<AiStudentInsightPayload> students) {
}
