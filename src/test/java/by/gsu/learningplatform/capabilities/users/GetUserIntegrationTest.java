package by.gsu.learningplatform.capabilities.users;

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
class GetUserIntegrationTest extends EndpointIntegrationTestSupport {

    private static final String USERS_PATH = "/users";
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
        val response = get(USERS_PATH + "/" + teacherId, null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn403ForStudentRole() {
        val response = get(USERS_PATH + "/" + teacherId, STUDENT_TOKEN);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldReturn200WhenTeacherReadsOwnProfile() {
        val response = get(USERS_PATH + "/" + teacherId, TEACHER_TOKEN);
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldReturn200WhenStudentReadsOwnProfile() {
        val response = get(USERS_PATH + "/" + studentId, STUDENT_TOKEN);
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldReturn200ForAdminRole() {
        val response = get(USERS_PATH + "/" + teacherId, ADMIN_TOKEN);
        assertEquals(200, response.statusCode());
    }

    @Test
    void shouldListUsersForAdminRole() throws Exception {
        val response = get(USERS_PATH + "?role=STUDENT&limit=10", ADMIN_TOKEN);

        assertEquals(200, response.statusCode());
        val body = json(response.body());
        assertEquals(1, body.get("items").size());
        assertEquals("student-user", body.get("items").get(0).get("username").asText());
    }

    @Test
    void shouldReturn403WhenTeacherListsUsers() {
        val response = get(USERS_PATH, TEACHER_TOKEN);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldReturnUserDetailsForAdminRole() throws Exception {
        val courseId = insertCourse(teacherId, "Student course");
        val taughtCourseId = insertCourse(studentId, "Unexpected taught course");
        val assessmentId = insertAssessment(taughtCourseId, studentId, "Created assessment");
        insertEnrollment(studentId, courseId);
        insertSubmission(assessmentId, studentId, 88);

        val response = get(USERS_PATH + "/" + studentId + "/details", ADMIN_TOKEN);

        assertEquals(200, response.statusCode());
        val body = json(response.body());
        assertEquals("student-user", body.get("user").get("username").asText());
        assertEquals(1, body.get("enrollments").size());
        assertEquals(1, body.get("enrolledCourses").size());
        assertEquals(1, body.get("taughtCourses").size());
        assertEquals(1, body.get("submissions").size());
        assertEquals(1, body.get("createdAssessments").size());
        assertTrue(body.get("enrolledCourses").get(0).has("tags"));
    }
}
