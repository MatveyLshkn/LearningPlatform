package by.gsu.learningplatform.capabilities.submissions;

import lombok.val;
import by.gsu.learningplatform.testsupport.EndpointIntegrationTestSupport;
import by.gsu.learningplatform.testsupport.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class SubmissionAuthorizationIntegrationTest extends EndpointIntegrationTestSupport {

    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private UUID assessmentId;

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        teacherId = insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        insertUser("other-teacher-sub", "other-teacher", "other-teacher@example.com", "TEACHER");
        studentId = insertUser("student-sub", "student-user", "student@example.com", "STUDENT");
        courseId = insertCourse(teacherId, "Submissions course");
        assessmentId = insertAssessment(courseId, teacherId, "Course assessment");
    }

    @Test
    void shouldForbidNonEnrolledStudentSubmission() {
        val response = post("/submissions", submissionPayload(), studentToken);

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldAllowEnrolledStudentSubmission() {
        insertEnrollment(studentId, courseId);

        val response = post("/submissions", submissionPayload(), studentToken);

        assertEquals(201, response.statusCode());
    }

    @Test
    void shouldForbidUnrelatedTeacherFromReadingAssessmentSubmissions() {
        insertSubmission(assessmentId, studentId, 90);

        val response = get("/submissions/assessment/" + assessmentId, "other-teacher-token");

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldAllowOwningTeacherToReadAssessmentSubmissions() {
        insertSubmission(assessmentId, studentId, 90);

        val response = get("/submissions/assessment/" + assessmentId, teacherToken);

        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldAllowAdminToReadAssessmentSubmissions() {
        insertSubmission(assessmentId, studentId, 90);

        val response = get("/submissions/assessment/" + assessmentId, adminToken);

        assertEquals(200, response.statusCode());
    }

    private String submissionPayload() {
        return "{\"assessmentId\":\"" + assessmentId + "\",\"answerText\":\"answer\"}";
    }
}
