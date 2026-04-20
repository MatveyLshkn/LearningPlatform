package by.gsu.learningplatform.capabilities.ai;

import java.util.List;
import java.util.UUID;

public record CourseStudentAnalyticsResponse(UUID courseId,
                                             Double averageScore,
                                             long totalSubmissions,
                                             long gradedSubmissions,
                                             Double recencyTrendDelta,
                                             String aiSummary,
                                             List<StudentAnalyticsResponse> students) {
}
