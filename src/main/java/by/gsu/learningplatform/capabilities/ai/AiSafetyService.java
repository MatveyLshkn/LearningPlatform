package by.gsu.learningplatform.capabilities.ai;

import lombok.val;
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

    public AiSafetyService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiAssessmentDraftPayload parseAssessmentDraft(final String json) {
        val node = parseRootObject(json);
        val title = readRequiredText(node, "title");
        val description = readRequiredText(node, "description");
        val questions = readFlexibleStringCollection(node.get("questions"));
        val answerKey = readFlexibleStringCollection(node.get("answerKey"));
        val rubricCriteria = readFlexibleStringCollection(node.get("rubricCriteria"));

        if (questions.isEmpty() || answerKey.isEmpty() || rubricCriteria.isEmpty()) {
            throw new AiIntegrationException("AI draft payload is incomplete");
        }

        return new AiAssessmentDraftPayload(title, description, questions, answerKey, rubricCriteria);
    }

    public AiAnalyticsPayload parseAnalyticsPayload(final String json) {
        val node = parseRootObject(json);
        val courseSummary = readRequiredText(node, "courseSummary");
        val studentsNode = node.get("students");
        if (studentsNode == null || !studentsNode.isArray()) {
            throw new AiIntegrationException("AI analytics payload is malformed");
        }

        val students = new ArrayList<AiStudentInsightPayload>();
        for (JsonNode studentNode : studentsNode) {
            val studentId = UUID.fromString(readRequiredText(studentNode, "studentId"));
            val focus = readRequiredText(studentNode, "improvementFocus");
            val confidence = studentNode.path("confidence").isNumber() ? studentNode.get("confidence").asDouble() : 0.5d;
            val actions = readStringArray(studentNode, "actions");
            students.add(new AiStudentInsightPayload(studentId, focus, clamp(confidence), actions));
        }
        return new AiAnalyticsPayload(courseSummary, students);
    }

    public AiStudyPlanPayload parseStudyPlan(final String json) {
        val node = parseRootObject(json);
        val goals = readStringArray(node, "prioritizedGoals");
        val weeklyTargets = readStringArray(node, "weeklyTargets");
        val lessonOrder = readStringArray(node, "lessonOrder");
        val lectureOrder = readStringArray(node, "lectureOrder");
        val rationale = readRequiredText(node, "rationale");
        if (goals.isEmpty() || weeklyTargets.isEmpty()) {
            throw new AiIntegrationException("AI study plan payload is incomplete");
        }
        return new AiStudyPlanPayload(goals, weeklyTargets, lessonOrder, lectureOrder, rationale);
    }

    private JsonNode parseRootObject(final String json) {
        val normalized = normalizeJsonBlock(json);
        try {
            val node = objectMapper.readTree(normalized);
            if (node == null || !node.isObject()) {
                throw new AiIntegrationException("AI payload is not a JSON object");
            }
            return node;
        } catch (Exception ex) {
            val extracted = extractFirstObject(normalized);
            if (extracted != null) {
                try {
                    val node = objectMapper.readTree(extracted);
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

    private String readRequiredText(final JsonNode node, final String fieldName) {
        val value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new AiIntegrationException("AI payload missing field: " + fieldName);
        }
        return value.asText().trim();
    }

    private List<String> readStringArray(final JsonNode node, final String fieldName) {
        val values = node.get(fieldName);
        return readFlexibleStringCollection(values);
    }

    private List<String> readFlexibleStringCollection(final JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        val result = new ArrayList<String>();
        if (node.isTextual()) {
            val value = node.asText().trim();
            if (!value.isBlank()) {
                result.add(value);
            }
            return result;
        }
        if (node.isArray()) {
            for (val item : node) {
                val mapped = mapNodeToText(item);
                if (!mapped.isBlank()) {
                    result.add(mapped);
                }
            }
            return result;
        }
        if (node.isObject()) {
            final Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                val field = names.next();
                val mapped = mapNodeToText(node.get(field));
                if (!mapped.isBlank()) {
                    result.add(field + ": " + mapped);
                }
            }
            return result;
        }
        return List.of();
    }

    private String mapNodeToText(final JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText().trim();
        }
        if (node.isObject()) {
            val text = node.get("text");
            if (text != null && text.isTextual() && !text.asText().isBlank()) {
                return text.asText().trim();
            }
            val keyPoints = node.get("key_points");
            if (keyPoints != null && keyPoints.isArray()) {
                val points = new ArrayList<String>();
                for (val point : keyPoints) {
                    val mapped = mapNodeToText(point);
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

    private String normalizeJsonBlock(final String value) {
        if (value == null) {
            return "";
        }
        var trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            val firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd >= 0) {
                trimmed = trimmed.substring(firstLineEnd + 1).trim();
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String extractFirstObject(final String value) {
        val start = value.indexOf('{');
        val end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return value.substring(start, end + 1);
    }

    private double clamp(final double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }
}
