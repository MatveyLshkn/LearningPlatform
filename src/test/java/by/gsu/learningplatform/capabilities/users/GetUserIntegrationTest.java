package by.gsu.learningplatform.capabilities.users;

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
class GetUserIntegrationTest extends EndpointIntegrationTestSupport {

    private static final String usersPath = "/users";
    private UUID teacherId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        teacherId = insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        studentId = insertUser("student-sub", "student-user", "student@example.com", "STUDENT");
    }

    @Test
    void shouldReturn401WhenNotLoggedIn() {
        final var response = get(usersPath + "/" + teacherId, null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn403ForStudentRole() {
        final var response = get(usersPath + "/" + teacherId, studentToken);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldReturn200WhenTeacherReadsOwnProfile() {
        final var response = get(usersPath + "/" + teacherId, teacherToken);
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldReturn200WhenStudentReadsOwnProfile() {
        final var response = get(usersPath + "/" + studentId, studentToken);
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldReturn200ForAdminRole() {
        final var response = get(usersPath + "/" + teacherId, adminToken);
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldListUsersForAdminRole() throws Exception {
        final var response = get(usersPath + "?role=STUDENT&limit=10", adminToken);

        assertEquals(200, response.statusCode());
        final var body = json(response.body());
        assertEquals(1, body.get("items").size());
        assertEquals("student-user", body.get("items").get(0).get("username").asText());
    }

    @Test
    void shouldReturn403WhenTeacherListsUsers() {
        final var response = get(usersPath, teacherToken);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldReturnUserDetailsForAdminRole() throws Exception {
        final var courseId = insertCourse(teacherId, "Student course");
        final var taughtCourseId = insertCourse(studentId, "Unexpected taught course");
        final var assessmentId = insertAssessment(taughtCourseId, studentId, "Created assessment");
        insertEnrollment(studentId, courseId);
        insertSubmission(assessmentId, studentId, 88);

        final var response = get(usersPath + "/" + studentId + "/details", adminToken);

        assertEquals(200, response.statusCode());
        final var body = json(response.body());
        assertEquals("student-user", body.get("user").get("username").asText());
        assertEquals(1, body.get("enrollments").size());
        assertEquals(1, body.get("enrolledCourses").size());
        assertEquals(1, body.get("taughtCourses").size());
        assertEquals(1, body.get("submissions").size());
        assertEquals(1, body.get("createdAssessments").size());
        assertTrue(body.get("enrolledCourses").get(0).has("tags"));
    }
}
