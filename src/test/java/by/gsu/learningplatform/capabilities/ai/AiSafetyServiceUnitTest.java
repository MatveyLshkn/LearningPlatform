package by.gsu.learningplatform.capabilities.ai;

import lombok.val;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiSafetyServiceUnitTest {

    private final AiSafetyService service = new AiSafetyService(new ObjectMapper());

    @Test
    void shouldParseAssessmentDraftJson() {
        val json = """
                {
                  "title":"Intro Assessment",
                  "description":"Checks basics",
                  "questions":["Q1","Q2"],
                  "answerKey":["A1","A2"],
                  "rubricCriteria":["Correctness","Completeness"]
                }
                """;

        val payload = service.parseAssessmentDraft(json);

        assertEquals("Intro Assessment", payload.title());
        assertEquals(2, payload.questions().size());
    }

    @Test
    void shouldRejectInvalidAnalyticsJson() {
        assertThrows(AiIntegrationException.class, () -> service.parseAnalyticsPayload("{\"students\":\"bad\"}"));
    }

    @Test
    void shouldParseAnalyticsPayload() {
        val studentId = UUID.randomUUID();
        val json = """
                {
                  "courseSummary":"Summary",
                  "students":[
                    {
                      "studentId":"%s",
                      "improvementFocus":"Fundamentals",
                      "confidence":0.75,
                      "actions":["Practice daily"]
                    }
                  ]
                }
                """.formatted(studentId);

        val payload = service.parseAnalyticsPayload(json);

        assertEquals("Summary", payload.courseSummary());
        assertEquals(1, payload.students().size());
        assertEquals(studentId, payload.students().getFirst().studentId());
    }
}
