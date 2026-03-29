package by.gsu.learningplatform.capabilities.enrollments;

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

    private static final String enrollmentsPath = "/enrollments";
    private UUID courseId;

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        final var teacherId = insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        insertUser("student-sub", "student-user", "student@example.com", "STUDENT");
        courseId = insertCourse(teacherId, "Course for enrollment");
    }

    @Test
    void shouldReturn401WhenNotLoggedIn() {
        final var response = post(enrollmentsPath, "{\"courseId\":\"" + courseId + "\"}", null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn403ForTeacherRole() {
        final var response = post(enrollmentsPath, "{\"courseId\":\"" + courseId + "\"}", teacherToken);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldCreateEnrollmentForStudentRole() throws Exception {
        final var response = post(enrollmentsPath, "{\"courseId\":\"" + courseId + "\"}", studentToken);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
    }

    @Test
    void shouldCreateEnrollmentForAdminRole() throws Exception {
        final var response = post(enrollmentsPath, "{\"courseId\":\"" + courseId + "\"}", adminToken);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
    }
}
