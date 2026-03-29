package by.gsu.learningplatform.capabilities.admin;

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
class GetPlatformStatisticsIntegrationTest extends EndpointIntegrationTestSupport {

    private static final String platformStatisticsPath = "/platform-statistics";

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        insertUser("student-sub", "student-user", "student@example.com", "STUDENT");
    }

    @Test
    void shouldReturn401WhenNotLoggedIn() {
        final var response = get(platformStatisticsPath, null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn403ForTeacherRole() {
        final var response = get(platformStatisticsPath, teacherToken);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldReturn403ForStudentRole() {
        final var response = get(platformStatisticsPath, studentToken);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldReturn200ForAdminRole() throws Exception {
        final var response = get(platformStatisticsPath, adminToken);
        assertEquals(200, response.statusCode());
        assertTrue(json(response.body()).has("usersCount"));
        assertTrue(json(response.body()).has("coursesCount"));
        assertTrue(json(response.body()).has("enrollmentsCount"));
    }
}
