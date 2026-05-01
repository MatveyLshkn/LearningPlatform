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
    void shouldListCoursesPubliclyWhenNotLoggedIn() throws Exception {
        insertCourse(teacherId, "Public Course");

        final var response = get(coursesPath, null);

        assertEquals(200, response.statusCode());
        assertEquals(1, json(response.body()).get("items").size());
    }

    @Test
    void shouldFilterCoursesPubliclyWhenNotLoggedIn() throws Exception {
        final var javaCourseId = insertCourse(teacherId, "Java Course");
        final var databasesCourseId = insertCourse(teacherId, "Databases Course");
        final var springTagId = insertTag("spring");
        final var postgresTagId = insertTag("postgres");
        insertCourseTag(javaCourseId, springTagId);
        insertCourseTag(databasesCourseId, postgresTagId);

        final var response = get(coursesPath + "?tag=spring", null);

        assertEquals(200, response.statusCode());
        final var items = json(response.body()).get("items");
        assertEquals(1, items.size());
        assertEquals("Java Course", items.get(0).get("title").asText());
    }

    @Test
    void shouldReturnPublicCatalogDetailsWithoutContentFields() throws Exception {
        final var courseId = insertCourse(teacherId, "Catalog Course");
        final var lessonId = insertLesson(courseId, "Catalog Lesson");
        insertLecture(lessonId, "Catalog Lecture");

        final var response = get(coursesPath + "/" + courseId, null);

        assertEquals(200, response.statusCode());
        final var body = json(response.body());
        assertEquals("Catalog Course", body.get("course").get("title").asText());
        final var lesson = body.get("lessons").get(0);
        assertEquals("Catalog Lesson", lesson.get("title").asText());
        assertTrue(!lesson.has("content"));
        final var lecture = lesson.get("lectures").get(0);
        assertEquals("Catalog Lecture", lecture.get("title").asText());
        assertTrue(!lecture.has("content"));
        assertTrue(!lecture.has("videoUrl"));
    }

    @Test
    void shouldRequireAuthenticationWhenListingTeacherCourses() {
        final var response = get(coursesPath + "/me", null);

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldListOnlyCurrentStudentEnrolledCourses() throws Exception {
        final var enrolledCourseId = insertCourse(teacherId, "Enrolled Course");
        insertCourse(teacherId, "Available Course");
        insertEnrollment(studentId, enrolledCourseId);

        final var response = get(coursesPath + "/enrolled/me", studentToken);

        assertEquals(200, response.statusCode());
        final var items = json(response.body()).get("items");
        assertEquals(1, items.size());
        assertTrue(items.get(0).has("enrollmentId"));
        assertEquals("Enrolled Course", items.get(0).get("course").get("title").asText());
    }

    @Test
    void shouldForbidTeacherWhenListingEnrolledCourses() {
        final var response = get(coursesPath + "/enrolled/me", teacherToken);

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldForbidNonEnrolledStudentFromReadingFullCourseContent() {
        final var courseId = insertCourse(teacherId, "Private Content Course");

        final var response = get(coursesPath + "/" + courseId + "/lessons", studentToken);

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldAllowEnrolledStudentToReadFullCourseContent() throws Exception {
        final var courseId = insertCourse(teacherId, "Private Content Course");
        final var lessonId = insertLesson(courseId, "Private Lesson");
        insertLecture(lessonId, "Private Lecture");
        insertEnrollment(studentId, courseId);

        final var lessonsResponse = get(coursesPath + "/" + courseId + "/lessons", studentToken);
        final var lecturesResponse = get("/lessons/" + lessonId + "/lectures", studentToken);

        assertEquals(200, lessonsResponse.statusCode());
        assertEquals("seeded lesson content", json(lessonsResponse.body()).get(0).get("content").asText());
        assertEquals(200, lecturesResponse.statusCode());
        assertEquals("seeded lecture content", json(lecturesResponse.body()).get(0).get("content").asText());
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
