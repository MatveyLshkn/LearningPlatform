package by.gsu.learningplatform.capabilities.courses;

import by.gsu.learningplatform.testsupport.EndpointIntegrationTestSupport;
import by.gsu.learningplatform.testsupport.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class CreateCourseIntegrationTest extends EndpointIntegrationTestSupport {

    private static final String coursesPath = "/courses";
    private static final String createCoursePayload = "{\"title\":\"Java\",\"description\":\"desc\"}";

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        insertUser("student-sub", "student-user", "student@example.com", "STUDENT");
    }

    @Test
    void shouldReturn401WhenNotLoggedIn() {
        final var response = post(coursesPath, createCoursePayload, null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn403ForStudentRole() {
        final var response = post(coursesPath, createCoursePayload, studentToken);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldCreateCourseForTeacherRole() throws Exception {
        final var response = post(coursesPath, createCoursePayload, teacherToken);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
    }

    @Test
    void shouldCreateCourseForAdminRole() throws Exception {
        final var response = post(coursesPath, "{\"title\":\"Architecture\",\"description\":\"desc\"}", adminToken);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
    }
}
