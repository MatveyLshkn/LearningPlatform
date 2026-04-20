package by.gsu.learningplatform.capabilities.ai;

import java.util.List;
import java.util.UUID;

public record AiStudentInsightPayload(UUID studentId,
                                      String improvementFocus,
                                      Double confidence,
                                      List<String> actions) {
}
