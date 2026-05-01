package by.gsu.learningplatform.capabilities.courses;

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
class CreateCourseIntegrationTest extends EndpointIntegrationTestSupport {

    private static final String COURSES_PATH = "/courses";
    private static final String CREATE_COURSE_PAYLOAD = "{\"title\":\"Java\",\"description\":\"desc\",\"tags\":[\"Java\",\" spring \"]}";
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
        val response = post(COURSES_PATH, CREATE_COURSE_PAYLOAD, null);
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldReturn403ForStudentRole() {
        val response = post(COURSES_PATH, CREATE_COURSE_PAYLOAD, STUDENT_TOKEN);
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldCreateCourseForTeacherRole() throws Exception {
        val response = post(COURSES_PATH, CREATE_COURSE_PAYLOAD, TEACHER_TOKEN);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
        assertEquals("java", json(response.body()).get("tags").get(0).asText());
        assertEquals("spring", json(response.body()).get("tags").get(1).asText());
    }

    @Test
    void shouldCreateCourseForAdminRole() throws Exception {
        val response = post(COURSES_PATH, "{\"title\":\"Architecture\",\"description\":\"desc\",\"tags\":[\"architecture\"]}", ADMIN_TOKEN);
        assertEquals(201, response.statusCode());
        assertTrue(json(response.body()).has("id"));
    }

    @Test
    void shouldFilterCoursesByAnyTag() throws Exception {
        val teacherId = jdbcTemplate.queryForObject(
                "select id from users where keycloak_sub = 'teacher-sub'",
                java.util.UUID.class);
        val javaCourseId = insertCourse(teacherId, "Java Course");
        val databasesCourseId = insertCourse(teacherId, "Databases Course");
        val javaTagId = insertTag("java");
        val springTagId = insertTag("spring");
        val postgresTagId = insertTag("postgres");
        insertCourseTag(javaCourseId, javaTagId);
        insertCourseTag(javaCourseId, springTagId);
        insertCourseTag(databasesCourseId, postgresTagId);

        val response = get(COURSES_PATH + "?tag=spring&tag=postgres", STUDENT_TOKEN);

        assertEquals(200, response.statusCode());
        assertEquals(2, json(response.body()).get("items").size());
    }

    @Test
    void shouldListCoursesPubliclyWhenNotLoggedIn() throws Exception {
        insertCourse(teacherId, "Public Course");

        val response = get(COURSES_PATH, null);

        assertEquals(200, response.statusCode());
        assertEquals(1, json(response.body()).get("items").size());
    }

    @Test
    void shouldFilterCoursesPubliclyWhenNotLoggedIn() throws Exception {
        val javaCourseId = insertCourse(teacherId, "Java Course");
        val databasesCourseId = insertCourse(teacherId, "Databases Course");
        val springTagId = insertTag("spring");
        val postgresTagId = insertTag("postgres");
        insertCourseTag(javaCourseId, springTagId);
        insertCourseTag(databasesCourseId, postgresTagId);

        val response = get(COURSES_PATH + "?tag=spring", null);

        assertEquals(200, response.statusCode());
        val items = json(response.body()).get("items");
        assertEquals(1, items.size());
        assertEquals("Java Course", items.get(0).get("title").asText());
    }

    @Test
    void shouldReturnPublicCatalogDetailsWithoutContentFields() throws Exception {
        val courseId = insertCourse(teacherId, "Catalog Course");
        val lessonId = insertLesson(courseId, "Catalog Lesson");
        insertLecture(lessonId, "Catalog Lecture");

        val response = get(COURSES_PATH + "/" + courseId, null);

        assertEquals(200, response.statusCode());
        val body = json(response.body());
        assertEquals("Catalog Course", body.get("course").get("title").asText());
        val lesson = body.get("lessons").get(0);
        assertEquals("Catalog Lesson", lesson.get("title").asText());
        assertTrue(!lesson.has("content"));
        val lecture = lesson.get("lectures").get(0);
        assertEquals("Catalog Lecture", lecture.get("title").asText());
        assertTrue(!lecture.has("content"));
        assertTrue(!lecture.has("videoUrl"));
    }

    @Test
    void shouldRequireAuthenticationWhenListingTeacherCourses() {
        val response = get(COURSES_PATH + "/me", null);

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldListOnlyCurrentStudentEnrolledCourses() throws Exception {
        val enrolledCourseId = insertCourse(teacherId, "Enrolled Course");
        insertCourse(teacherId, "Available Course");
        insertEnrollment(studentId, enrolledCourseId);

        val response = get(COURSES_PATH + "/enrolled/me", STUDENT_TOKEN);

        assertEquals(200, response.statusCode());
        val items = json(response.body()).get("items");
        assertEquals(1, items.size());
        assertTrue(items.get(0).has("enrollmentId"));
        assertEquals("Enrolled Course", items.get(0).get("course").get("title").asText());
    }

    @Test
    void shouldForbidTeacherWhenListingEnrolledCourses() {
        val response = get(COURSES_PATH + "/enrolled/me", TEACHER_TOKEN);

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldForbidNonEnrolledStudentFromReadingFullCourseContent() {
        val courseId = insertCourse(teacherId, "Private Content Course");

        val response = get(COURSES_PATH + "/" + courseId + "/lessons", STUDENT_TOKEN);

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldAllowEnrolledStudentToReadFullCourseContent() throws Exception {
        val courseId = insertCourse(teacherId, "Private Content Course");
        val lessonId = insertLesson(courseId, "Private Lesson");
        insertLecture(lessonId, "Private Lecture");
        insertEnrollment(studentId, courseId);

        val lessonsResponse = get(COURSES_PATH + "/" + courseId + "/lessons", STUDENT_TOKEN);
        val lecturesResponse = get("/lessons/" + lessonId + "/lectures", STUDENT_TOKEN);

        assertEquals(200, lessonsResponse.statusCode());
        assertEquals("seeded lesson content", json(lessonsResponse.body()).get(0).get("content").asText());
        assertEquals(200, lecturesResponse.statusCode());
        assertEquals("seeded lecture content", json(lecturesResponse.body()).get(0).get("content").asText());
    }

    @Test
    void shouldReplaceCourseTagsOnPatch() throws Exception {
        val createResponse = post(COURSES_PATH, CREATE_COURSE_PAYLOAD, TEACHER_TOKEN);
        val courseId = json(createResponse.body()).get("id").asText();

        val response = patch(
                COURSES_PATH + "/" + courseId,
                "{\"title\":\"Updated\",\"description\":\"desc\",\"tags\":[\"Architecture\"]}",
                TEACHER_TOKEN);

        assertEquals(200, response.statusCode());
        val tags = json(response.body()).get("tags");
        assertEquals(1, tags.size());
        assertEquals("architecture", tags.get(0).asText());
    }

    @Test
    void shouldListOnlyCurrentTeacherCourses() throws Exception {
        val otherTeacherId = insertUser("other-teacher-sub", "other-teacher", "other-teacher@example.com", "TEACHER");
        insertCourse(teacherId, "Own Course");
        insertCourse(otherTeacherId, "Other Course");

        val response = get(COURSES_PATH + "/me", TEACHER_TOKEN);

        assertEquals(200, response.statusCode());
        val items = json(response.body()).get("items");
        assertEquals(1, items.size());
        assertEquals("Own Course", items.get(0).get("title").asText());
        assertEquals(teacherId.toString(), items.get(0).get("teacherId").asText());
    }

    @Test
    void shouldForbidStudentWhenListingOwnCreatedCourses() {
        val response = get(COURSES_PATH + "/me", STUDENT_TOKEN);

        assertEquals(403, response.statusCode());
    }
}
