package by.gsu.learningplatform.capabilities.ai;

import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import org.springframework.stereotype.Service;

@Service
public class AiPromptBuilderService {

    private final int maxPromptChars;

    public AiPromptBuilderService(final LearningPlatformProperties properties) {
        this.maxPromptChars = properties.ai().maxPromptChars();
    }

    public String assessmentDraftSystemPrompt() {
        return """
                You create high-quality assessments for a learning platform.
                Always answer with valid JSON object only.
                JSON schema:
                {
                  "title": "string",
                  "description": "string",
                  "questions": ["string"],
                  "answerKey": ["string"],
                  "rubricCriteria": ["string"]
                }
                Keep question count equal to the requested count.
                """;
    }

    public String buildAssessmentDraftUserPrompt(final String source, final int questionCount, final String difficulty) {
        return truncate("""
                Generate an assessment draft from the provided course material.
                Question count: %d
                Difficulty: %s
                Focus on conceptual understanding and practical checks.

                Material:
                %s
                """.formatted(questionCount, difficulty, source));
    }

    public String analyticsSystemPrompt() {
        return """
                You are an education analytics assistant for teachers.
                Always answer with valid JSON object only.
                JSON schema:
                {
                  "courseSummary": "string",
                  "students": [
                    {
                      "studentId": "uuid",
                      "improvementFocus": "string",
                      "confidence": 0.0,
                      "actions": ["string"]
                    }
                  ]
                }
                Keep confidence in [0.0, 1.0].
                """;
    }

    public String buildAnalyticsUserPrompt(final String deterministicSummary) {
        return truncate("""
                Analyze student performance data and provide actionable guidance.
                Use only student IDs provided in the input.

                Data:
                %s
                """.formatted(deterministicSummary));
    }

    public String studyPlanSystemPrompt() {
        return """
                You create personalized study plans.
                Always answer with valid JSON object only.
                JSON schema:
                {
                  "prioritizedGoals": ["string"],
                  "weeklyTargets": ["string"],
                  "lessonOrder": ["string"],
                  "lectureOrder": ["string"],
                  "rationale": "string"
                }
                Weekly targets must be measurable.
                """;
    }

    public String buildStudyPlanUserPrompt(final String studentSummary, final String courseMaterials) {
        return truncate("""
                Build a personalized plan for one student in this course.

                Student performance summary:
                %s

                Course material references:
                %s
                """.formatted(studentSummary, courseMaterials));
    }

    public String truncate(final String value) {
        if (value.length() <= maxPromptChars) {
            return value;
        }
        return value.substring(0, maxPromptChars);
    }
}
