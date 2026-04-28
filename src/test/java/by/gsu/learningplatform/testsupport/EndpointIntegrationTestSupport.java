package by.gsu.learningplatform.testsupport;

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

    protected static final String adminToken = "admin-token";
    protected static final String teacherToken = "teacher-token";
    protected static final String studentToken = "student-token";
    private static final String localHostBaseUrl = "http://localhost:";

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

    protected UUID insertUser(String sub, String username, String email, String role) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users(id, keycloak_sub, username, email, role, created_at) values (?,?,?,?,?,CURRENT_TIMESTAMP)",
                id, sub, username, email, role);
        return id;
    }

    protected UUID insertCourse(UUID teacherId, String title) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into courses(id, title, description, teacher_id, created_at) values (?,?,?,?,CURRENT_TIMESTAMP)",
                id, title, "seeded", teacherId);
        return id;
    }

    protected UUID insertLesson(UUID courseId, String title) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into lessons(id, course_id, title, content) values (?,?,?,?)",
                id, courseId, title, "seeded lesson content");
        return id;
    }

    protected UUID insertLecture(UUID lessonId, String title) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into lectures(id, lesson_id, title, content) values (?,?,?,?)",
                id, lessonId, title, "seeded lecture content");
        return id;
    }

    protected UUID insertEnrollment(UUID userId, UUID courseId) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into enrollments(id, user_id, course_id, enrolled_at) values (?,?,?,CURRENT_TIMESTAMP)",
                id, userId, courseId);
        return id;
    }

    protected UUID insertAssessment(UUID courseId, UUID createdBy, String title) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into assessments(id, course_id, created_by, title, description, created_at)
                        values (?,?,?,?,?,CURRENT_TIMESTAMP)
                        """,
                id, courseId, createdBy, title, "seeded assessment");
        return id;
    }

    protected UUID insertSubmission(UUID assessmentId, UUID studentId, int score) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into submissions(id, assessment_id, student_id, answer_text, score, submitted_at, graded_at)
                        values (?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                        """,
                id, assessmentId, studentId, "seeded answer", score);
        return id;
    }

    protected UUID insertTag(String name) {
        final var id = UUID.randomUUID();
        jdbcTemplate.update("insert into tags(id, name) values (?,?)", id, name);
        return id;
    }

    protected void insertCourseTag(UUID courseId, UUID tagId) {
        jdbcTemplate.update("insert into course_tags(course_id, tag_id) values (?,?)", courseId, tagId);
    }

    protected ApiResult get(String path, String token) {
        return request(path, HttpMethod.GET, null, token);
    }

    protected ApiResult post(String path, String body, String token) {
        return request(path, HttpMethod.POST, body, token);
    }

    protected ApiResult patch(String path, String body, String token) {
        return request(path, HttpMethod.PATCH, body, token);
    }

    protected ApiResult put(String path, String body, String token) {
        return request(path, HttpMethod.PUT, body, token);
    }

    protected JsonNode json(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private ApiResult request(String path, HttpMethod method, String body, String token) {
        final var client = RestClient.create();
        final var url = localHostBaseUrl + port + path;

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

    private void addBearer(HttpHeaders headers, String token) {
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
    }

    private ApiResult toResult(org.springframework.http.client.ClientHttpResponse response) throws IOException {
        final var responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        return new ApiResult(response.getStatusCode().value(), responseBody);
    }

    protected record ApiResult(int statusCode, String body) {
    }
}
