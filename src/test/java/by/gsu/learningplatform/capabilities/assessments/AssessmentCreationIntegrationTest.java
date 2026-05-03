package by.gsu.learningplatform.capabilities.assessments;

import lombok.val;
import by.gsu.learningplatform.testsupport.EndpointIntegrationTestSupport;
import by.gsu.learningplatform.testsupport.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class AssessmentCreationIntegrationTest extends EndpointIntegrationTestSupport {

    private UUID teacherId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        teacherId = insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        courseId = insertCourse(teacherId, "Assessment course");
    }

    @Test
    void shouldPopulateCreatedAtWhenCreateAssessment() throws Exception {
        val response = post("/assessments", createPayload(), teacherToken);

        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).hasNonNull("createdAt"));
    }

    @Test
    void shouldPopulateCreatedAtWhenCreateAssessmentFromDraft() throws Exception {
        val response = post("/assessments/from-draft", createFromDraftPayload(), teacherToken);

        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).hasNonNull("createdAt"));
    }

    private String createPayload() {
        return """
                {
                  "courseId":"%s",
                  "title":"Assessment title",
                  "description":"Assessment description",
                  "questions":["What is JVM?"],
                  "answerKey":["Java Virtual Machine"],
                  "rubricCriteria":["Mentions runtime and bytecode execution"]
                }
                """.formatted(courseId);
    }

    private String createFromDraftPayload() {
        return """
                {
                  "courseId":"%s",
                  "title":"Draft title",
                  "description":"Draft description",
                  "questions":["Explain polymorphism"],
                  "answerKey":["Ability to process objects via common interface"],
                  "rubricCriteria":["Uses clear OOP example"]
                }
                """.formatted(courseId);
    }
}
