package by.gsu.learningplatform.testsupport;

import lombok.val;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public abstract class EndpointIntegrationTestSupport {

    protected static final String ADMIN_TOKEN = "admin-token";
    protected static final String TEACHER_TOKEN = "teacher-token";
    protected static final String STUDENT_TOKEN = "student-token";
    protected static final String adminToken = ADMIN_TOKEN;
    protected static final String teacherToken = TEACHER_TOKEN;
    protected static final String studentToken = STUDENT_TOKEN;
    private static final String LOCAL_HOST_BASE_URL = "http://localhost:";

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected void clearAllTables() {
        jdbcTemplate.update("delete from lecture_progress");
        jdbcTemplate.update("delete from submissions");
        jdbcTemplate.update("delete from assessments");
        jdbcTemplate.update("delete from lectures");
        jdbcTemplate.update("delete from lessons");
        jdbcTemplate.update("delete from enrollments");
        jdbcTemplate.update("delete from course_tags");
        jdbcTemplate.update("delete from tags");
        jdbcTemplate.update("delete from courses");
        jdbcTemplate.update("delete from users");
    }

    protected UUID insertUser(final String sub, final String username, final String email, final String role) {
        val id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users(id, keycloak_sub, username, email, role, created_at) values (?,?,?,?,?,CURRENT_TIMESTAMP)",
                id, sub, username, email, role);
        return id;
    }

    protected UUID insertCourse(final UUID teacherId, final String title) {
        val id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into courses(id, title, description, teacher_id, created_at) values (?,?,?,?,CURRENT_TIMESTAMP)",
                id, title, "seeded", teacherId);
        return id;
    }

    protected UUID insertLesson(final UUID courseId, final String title) {
        val id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into lessons(id, course_id, title, content) values (?,?,?,?)",
                id, courseId, title, "seeded lesson content");
        return id;
    }

    protected UUID insertLecture(final UUID lessonId, final String title) {
        val id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into lectures(id, lesson_id, title, content) values (?,?,?,?)",
                id, lessonId, title, "seeded lecture content");
        return id;
    }

    protected UUID insertEnrollment(final UUID userId, final UUID courseId) {
        val id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into enrollments(id, user_id, course_id, enrolled_at) values (?,?,?,CURRENT_TIMESTAMP)",
                id, userId, courseId);
        return id;
    }

    protected UUID insertAssessment(final UUID courseId, final UUID createdBy, final String title) {
        val id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into assessments(id, course_id, created_by, title, description, created_at)
                        values (?,?,?,?,?,CURRENT_TIMESTAMP)
                        """,
                id, courseId, createdBy, title, "seeded assessment");
        return id;
    }

    protected UUID insertSubmission(final UUID assessmentId, final UUID studentId, final int score) {
        val id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into submissions(id, assessment_id, student_id, answer_text, score, submitted_at, graded_at)
                        values (?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                        """,
                id, assessmentId, studentId, "seeded answer", score);
        return id;
    }

    protected UUID insertTag(final String name) {
        val id = UUID.randomUUID();
        jdbcTemplate.update("insert into tags(id, name) values (?,?)", id, name);
        return id;
    }

    protected void insertCourseTag(final UUID courseId, final UUID tagId) {
        jdbcTemplate.update("insert into course_tags(course_id, tag_id) values (?,?)", courseId, tagId);
    }

    protected ApiResult get(final String path, final String token) {
        return request(path, HttpMethod.GET, null, token);
    }

    protected ApiResult post(final String path, final String body, final String token) {
        return request(path, HttpMethod.POST, body, token);
    }

    protected ApiResult patch(final String path, final String body, final String token) {
        return request(path, HttpMethod.PATCH, body, token);
    }

    protected ApiResult put(final String path, final String body, final String token) {
        return request(path, HttpMethod.PUT, body, token);
    }

    protected JsonNode json(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private ApiResult request(final String path, final HttpMethod method, final String body, final String token) {
        val client = RestClient.create();
        val url = LOCAL_HOST_BASE_URL + port + path;

        if (HttpMethod.GET.equals(method)) {
            return client.get()
                    .uri(url)
                    .headers(headers -> addBearer(headers, token))
                    .exchange((request, response) -> toResult(response));
        }
        if (HttpMethod.POST.equals(method)) {
            return client.post()
                    .uri(url)
                    .headers(headers -> addBearer(headers, token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body == null ? "" : body)
                    .exchange((request, response) -> toResult(response));
        }
        if (HttpMethod.PATCH.equals(method)) {
            return client.patch()
                    .uri(url)
                    .headers(headers -> addBearer(headers, token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body == null ? "" : body)
                    .exchange((request, response) -> toResult(response));
        }
        if (HttpMethod.PUT.equals(method)) {
            return client.put()
                    .uri(url)
                    .headers(headers -> addBearer(headers, token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body == null ? "" : body)
                    .exchange((request, response) -> toResult(response));
        }
        throw new IllegalArgumentException("Unsupported method: " + method);
    }

    private void addBearer(final HttpHeaders headers, final String token) {
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
    }

    private ApiResult toResult(org.springframework.http.client.ClientHttpResponse response) throws IOException {
        val responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        return new ApiResult(response.getStatusCode().value(), responseBody);
    }

    protected record ApiResult(int statusCode, String body) {
    }
}
