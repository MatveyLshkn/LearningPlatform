package by.gsu.learningplatform.capabilities.ai;

import java.util.List;
import java.util.UUID;

public record StudentAnalyticsResponse(UUID studentId,
                                       int totalSubmissions,
                                       int gradedSubmissions,
                                       Double averageScore,
                                       String trend,
                                       String improvementFocus,
                                       Double confidence,
                                       List<String> actions) {
}
