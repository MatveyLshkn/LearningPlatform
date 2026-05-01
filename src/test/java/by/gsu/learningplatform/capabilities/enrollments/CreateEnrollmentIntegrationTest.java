package by.gsu.learningplatform.capabilities.enrollments;

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
class CreateEnrollmentIntegrationTest extends EndpointIntegrationTestSupport {

    private static final String ENROLLMENTS_PATH = "/enrollments";
    private UUID courseId;

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        val teacherId = insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        insertUser("student-sub", "student-user", "student@example.com", "STUDENT");
        courseId = insertCourse(teacherId, "Course for enrollment");
    }

    @Test
    void shouldReturn401WhenNotLoggedIn() {
        val response = post(ENROLLMENTS_PATH, "{\"courseId\":\"" + courseId + "\"}", null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn403ForTeacherRole() {
        val response = post(ENROLLMENTS_PATH, "{\"courseId\":\"" + courseId + "\"}", TEACHER_TOKEN);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldCreateEnrollmentForStudentRole() throws Exception {
        val response = post(ENROLLMENTS_PATH, "{\"courseId\":\"" + courseId + "\"}", STUDENT_TOKEN);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
    }

    @Test
    void shouldCreateEnrollmentForAdminRole() throws Exception {
        val response = post(ENROLLMENTS_PATH, "{\"courseId\":\"" + courseId + "\"}", ADMIN_TOKEN);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
    }
}
