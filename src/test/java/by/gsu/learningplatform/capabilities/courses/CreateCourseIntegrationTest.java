package by.gsu.learningplatform.capabilities.courses;

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
class CreateCourseIntegrationTest extends EndpointIntegrationTestSupport {

    private static final String coursesPath = "/courses";
    private static final String createCoursePayload = "{\"title\":\"Java\",\"description\":\"desc\",\"tags\":[\"Java\",\" spring \"]}";
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
        assertEquals("java", json(response.body()).get("tags").get(0).asText());
        assertEquals("spring", json(response.body()).get("tags").get(1).asText());
    }

    @Test
    void shouldCreateCourseForAdminRole() throws Exception {
        final var response = post(coursesPath, "{\"title\":\"Architecture\",\"description\":\"desc\",\"tags\":[\"architecture\"]}", adminToken);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
    }

    @Test
    void shouldFilterCoursesByAnyTag() throws Exception {
        final var teacherId = jdbcTemplate.queryForObject(
                "select id from users where keycloak_sub = 'teacher-sub'",
                java.util.UUID.class);
        final var javaCourseId = insertCourse(teacherId, "Java Course");
        final var databasesCourseId = insertCourse(teacherId, "Databases Course");
        final var javaTagId = insertTag("java");
        final var springTagId = insertTag("spring");
        final var postgresTagId = insertTag("postgres");
        insertCourseTag(javaCourseId, javaTagId);
        insertCourseTag(javaCourseId, springTagId);
        insertCourseTag(databasesCourseId, postgresTagId);

        final var response = get(coursesPath + "?tag=spring&tag=postgres", studentToken);

        assertEquals(200, response.statusCode());
        assertEquals(2, json(response.body()).get("items").size());
    }

    @Test
    void shouldReplaceCourseTagsOnPatch() throws Exception {
        final var createResponse = post(coursesPath, createCoursePayload, teacherToken);
        final var courseId = json(createResponse.body()).get("id").asText();

        final var response = patch(
                coursesPath + "/" + courseId,
                "{\"title\":\"Updated\",\"description\":\"desc\",\"tags\":[\"Architecture\"]}",
                teacherToken);

        assertEquals(200, response.statusCode());
        final var tags = json(response.body()).get("tags");
        assertEquals(1, tags.size());
        assertEquals("architecture", tags.get(0).asText());
    }

    @Test
    void shouldListOnlyCurrentTeacherCourses() throws Exception {
        final var otherTeacherId = insertUser("other-teacher-sub", "other-teacher", "other-teacher@example.com", "TEACHER");
        insertCourse(teacherId, "Own Course");
        insertCourse(otherTeacherId, "Other Course");

        final var response = get(coursesPath + "/me", teacherToken);

        assertEquals(200, response.statusCode());
        final var items = json(response.body()).get("items");
        assertEquals(1, items.size());
        assertEquals("Own Course", items.get(0).get("title").asText());
        assertEquals(teacherId.toString(), items.get(0).get("teacherId").asText());
    }

    @Test
    void shouldForbidStudentWhenListingOwnCreatedCourses() {
        final var response = get(coursesPath + "/me", studentToken);

        assertEquals(403, response.statusCode());
    }
}
