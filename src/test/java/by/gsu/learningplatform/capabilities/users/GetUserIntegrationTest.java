package by.gsu.learningplatform.capabilities.users;

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
class GetUserIntegrationTest extends EndpointIntegrationTestSupport {

    private static final String usersPath = "/users/";
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        teacherId = insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        insertUser("student-sub", "student-user", "student@example.com", "STUDENT");
    }

    @Test
    void shouldReturn401WhenNotLoggedIn() {
        final var response = get(usersPath + teacherId, null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn403ForStudentRole() {
        final var response = get(usersPath + teacherId, studentToken);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldReturn200ForTeacherRole() {
        final var response = get(usersPath + teacherId, teacherToken);
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldReturn200ForAdminRole() {
        final var response = get(usersPath + teacherId, adminToken);
        assertEquals(200, response.statusCode());
    }
}
