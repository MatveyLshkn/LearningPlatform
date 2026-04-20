package by.gsu.learningplatform.capabilities.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Service
public class AiSafetyService {

    private final ObjectMapper objectMapper;

    public AiSafetyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiAssessmentDraftPayload parseAssessmentDraft(String json) {
        final var node = parseRootObject(json);
        final var title = readRequiredText(node, "title");
        final var description = readRequiredText(node, "description");
        final var questions = readFlexibleStringCollection(node.get("questions"));
        final var answerKey = readFlexibleStringCollection(node.get("answerKey"));
        final var rubricCriteria = readFlexibleStringCollection(node.get("rubricCriteria"));

        if (questions.isEmpty() || answerKey.isEmpty() || rubricCriteria.isEmpty()) {
            throw new AiIntegrationException("AI draft payload is incomplete");
        }

        return new AiAssessmentDraftPayload(title, description, questions, answerKey, rubricCriteria);
    }

    public AiAnalyticsPayload parseAnalyticsPayload(String json) {
        final var node = parseRootObject(json);
        final var courseSummary = readRequiredText(node, "courseSummary");
        final var studentsNode = node.get("students");
        if (studentsNode == null || !studentsNode.isArray()) {
            throw new AiIntegrationException("AI analytics payload is malformed");
        }

        final var students = new ArrayList<AiStudentInsightPayload>();
        for (JsonNode studentNode : studentsNode) {
            final var studentId = UUID.fromString(readRequiredText(studentNode, "studentId"));
            final var focus = readRequiredText(studentNode, "improvementFocus");
            final var confidence = studentNode.path("confidence").isNumber() ? studentNode.get("confidence").asDouble() : 0.5d;
            final var actions = readStringArray(studentNode, "actions");
            students.add(new AiStudentInsightPayload(studentId, focus, clamp(confidence), actions));
        }
        return new AiAnalyticsPayload(courseSummary, students);
    }

    public AiStudyPlanPayload parseStudyPlan(String json) {
        final var node = parseRootObject(json);
        final var goals = readStringArray(node, "prioritizedGoals");
        final var weeklyTargets = readStringArray(node, "weeklyTargets");
        final var lessonOrder = readStringArray(node, "lessonOrder");
        final var lectureOrder = readStringArray(node, "lectureOrder");
        final var rationale = readRequiredText(node, "rationale");
        if (goals.isEmpty() || weeklyTargets.isEmpty()) {
            throw new AiIntegrationException("AI study plan payload is incomplete");
        }
        return new AiStudyPlanPayload(goals, weeklyTargets, lessonOrder, lectureOrder, rationale);
    }

    private JsonNode parseRootObject(String json) {
        var normalized = normalizeJsonBlock(json);
        try {
            final var node = objectMapper.readTree(normalized);
            if (node == null || !node.isObject()) {
                throw new AiIntegrationException("AI payload is not a JSON object");
            }
            return node;
        } catch (Exception ex) {
            final var extracted = extractFirstObject(normalized);
            if (extracted != null) {
                try {
                    final var node = objectMapper.readTree(extracted);
                    if (node != null && node.isObject()) {
                        return node;
                    }
                } catch (Exception ignored) {
                    // fall through
                }
            }
            throw new AiIntegrationException("AI payload is not valid JSON");
        }
    }

    private String readRequiredText(JsonNode node, String fieldName) {
        final var value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new AiIntegrationException("AI payload missing field: " + fieldName);
        }
        return value.asText().trim();
    }

    private List<String> readStringArray(JsonNode node, String fieldName) {
        final var values = node.get(fieldName);
        return readFlexibleStringCollection(values);
    }

    private List<String> readFlexibleStringCollection(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        final var result = new ArrayList<String>();
        if (node.isTextual()) {
            final var value = node.asText().trim();
            if (!value.isBlank()) {
                result.add(value);
            }
            return result;
        }
        if (node.isArray()) {
            for (var item : node) {
                final var mapped = mapNodeToText(item);
                if (!mapped.isBlank()) {
                    result.add(mapped);
                }
            }
            return result;
        }
        if (node.isObject()) {
            final Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                final var field = names.next();
                final var mapped = mapNodeToText(node.get(field));
                if (!mapped.isBlank()) {
                    result.add(field + ": " + mapped);
                }
            }
            return result;
        }
        return List.of();
    }

    private String mapNodeToText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText().trim();
        }
        if (node.isObject()) {
            final var text = node.get("text");
            if (text != null && text.isTextual() && !text.asText().isBlank()) {
                return text.asText().trim();
            }
            final var keyPoints = node.get("key_points");
            if (keyPoints != null && keyPoints.isArray()) {
                final var points = new ArrayList<String>();
                for (var point : keyPoints) {
                    final var mapped = mapNodeToText(point);
                    if (!mapped.isBlank()) {
                        points.add(mapped);
                    }
                }
                if (!points.isEmpty()) {
                    return String.join(" | ", points);
                }
            }
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return node.toString();
        }
    }

    private String normalizeJsonBlock(String value) {
        if (value == null) {
            return "";
        }
        var trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            final var firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd >= 0) {
                trimmed = trimmed.substring(firstLineEnd + 1).trim();
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String extractFirstObject(String value) {
        final var start = value.indexOf('{');
        final var end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return value.substring(start, end + 1);
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }
}
