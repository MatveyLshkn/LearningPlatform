package by.gsu.learningplatform.capabilities.progress;

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
class CourseProgressIntegrationTest extends EndpointIntegrationTestSupport {

    private UUID teacherId;
    private UUID otherTeacherId;
    private UUID studentId;
    private UUID courseId;
    private UUID lectureId;

    @BeforeEach
    void setUp() {
        clearAllTables();
        insertUser("admin-sub", "admin-user", "admin@example.com", "ADMIN");
        teacherId = insertUser("teacher-sub", "teacher-user", "teacher@example.com", "TEACHER");
        otherTeacherId = insertUser("other-teacher-sub", "other-teacher", "other-teacher@example.com", "TEACHER");
        studentId = insertUser("student-sub", "student-user", "student@example.com", "STUDENT");
        courseId = insertCourse(teacherId, "Progress course");
        final var lessonId = insertLesson(courseId, "Progress lesson");
        lectureId = insertLecture(lessonId, "Progress lecture");
        insertEnrollment(studentId, courseId);
    }

    @Test
    void shouldAllowStudentToTrackOwnProgress() throws Exception {
        final var update = put(progressLecturePath(studentId, lectureId), "{\"completed\":true}", studentToken);
        assertEquals(200, update.statusCode());
        var body = json(update.body());
        assertEquals(1, body.get("totalLectures").asInt());
        assertEquals(1, body.get("completedLectures").asInt());
        assertEquals(100.0, body.get("progressPercent").asDouble());

        final var read = get("/courses/" + courseId + "/progress/me", studentToken);
        assertEquals(200, read.statusCode());
        body = json(read.body());
        assertEquals(1, body.get("completedLectureIds").size());
    }

    @Test
    void shouldAllowCourseTeacherToReadStudentProgress() {
        final var studentUpdate = put(progressLecturePath(studentId, lectureId), "{\"completed\":true}", studentToken);
        assertEquals(200, studentUpdate.statusCode());

        final var read = get("/courses/" + courseId + "/users/" + studentId + "/progress", teacherToken);
        assertEquals(200, read.statusCode());
    }

    @Test
    void shouldForbidCourseTeacherWhenUpdatingStudentProgress() {
        final var update = put(progressLecturePath(studentId, lectureId), "{\"completed\":true}", teacherToken);
        assertEquals(403, update.statusCode());
    }

    @Test
    void shouldForbidNonOwnerTeacher() {
        final var response = get("/courses/" + courseId + "/users/" + studentId + "/progress", token("other-teacher-token"));
        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldRejectLectureFromAnotherCourse() {
        final var otherCourseId = insertCourse(otherTeacherId, "Other course");
        final var otherLessonId = insertLesson(otherCourseId, "Other lesson");
        final var otherLectureId = insertLecture(otherLessonId, "Other lecture");

        final var response = put(progressLecturePath(studentId, otherLectureId), "{\"completed\":true}", studentToken);
        assertEquals(400, response.statusCode());
    }

    private String progressLecturePath(UUID userId, UUID targetLectureId) {
        return "/courses/" + courseId + "/users/" + userId + "/progress/lectures/" + targetLectureId;
    }

    private String token(String value) {
        return value;
    }
}
