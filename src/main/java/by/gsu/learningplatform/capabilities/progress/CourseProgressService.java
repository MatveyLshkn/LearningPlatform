package by.gsu.learningplatform.capabilities.progress;

import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.lectures.LectureRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.BadRequestException;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CourseProgressService {

    private final LectureProgressRepository lectureProgressRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthFacade authFacade;

    public CourseProgressService(LectureProgressRepository lectureProgressRepository,
                                 LectureRepository lectureRepository,
                                 EnrollmentRepository enrollmentRepository,
                                 CourseService courseService,
                                 UserService userService,
                                 AuthFacade authFacade) {
        this.lectureProgressRepository = lectureProgressRepository;
        this.lectureRepository = lectureRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.authFacade = authFacade;
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse getOwnProgress(UUID courseId) {
        final var actor = currentUser();
        return getProgress(courseId, actor.getId());
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse getProgress(UUID courseId, UUID userId) {
        final var actor = currentUser();
        final var course = courseService.getEntity(courseId);
        final var user = userService.getById(userId);
        validateCanRead(actor, course, user);
        validateEnrollment(courseId, user);
        return buildResponse(courseId, userId);
    }

    @Transactional
    public CourseProgressResponse updateLectureProgress(UUID courseId,
                                                        UUID userId,
                                                        UUID lectureId,
                                                        LectureProgressUpdateRequest request) {
        final var actor = currentUser();
        final var course = courseService.getEntity(courseId);
        final var user = userService.getById(userId);
        validateCanWrite(actor, course, user);
        validateEnrollment(courseId, user);
        validateLectureBelongsToCourse(courseId, lectureId);

        if (request.completed()) {
            lectureProgressRepository.findByUserIdAndLectureId(userId, lectureId).orElseGet(() -> {
                final var progress = new LectureProgressEntity();
                progress.setUserId(userId);
                progress.setLectureId(lectureId);
                return lectureProgressRepository.save(progress);
            });
        } else {
            lectureProgressRepository.findByUserIdAndLectureId(userId, lectureId)
                    .ifPresent(lectureProgressRepository::delete);
        }

        return buildResponse(courseId, userId);
    }

    private CourseProgressResponse buildResponse(UUID courseId, UUID userId) {
        final var lectureIds = lectureRepository.findIdsByCourseId(courseId);
        if (lectureIds.isEmpty()) {
            return new CourseProgressResponse(courseId, userId, 0, 0, 0.0, List.of(), null);
        }

        final var progressRows = lectureProgressRepository.findByUserIdAndLectureIdIn(userId, lectureIds);
        final var completedLectureIds = progressRows.stream()
                .map(LectureProgressEntity::getLectureId)
                .sorted()
                .toList();
        final var lastCompletedAt = progressRows.stream()
                .map(LectureProgressEntity::getCompletedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        final var completedCount = completedLectureIds.size();
        final var percent = Math.round(((double) completedCount / lectureIds.size()) * 10000.0) / 100.0;

        return new CourseProgressResponse(
                courseId,
                userId,
                lectureIds.size(),
                completedCount,
                percent,
                completedLectureIds,
                lastCompletedAt);
    }

    private void validateCanRead(UserEntity actor, CourseEntity course, UserEntity user) {
        if (actor.getRole() == UserRole.ADMIN || actor.getId().equals(user.getId()) || actor.getId().equals(course.getTeacherId())) {
            return;
        }
        throw new ForbiddenException("You cannot view this user's course progress");
    }

    private void validateCanWrite(UserEntity actor, CourseEntity course, UserEntity user) {
        if (actor.getRole() == UserRole.ADMIN || actor.getId().equals(user.getId()) || actor.getId().equals(course.getTeacherId())) {
            return;
        }
        throw new ForbiddenException("You cannot update this user's course progress");
    }

    private void validateLectureBelongsToCourse(UUID courseId, UUID lectureId) {
        if (!lectureRepository.existsById(lectureId)) {
            throw new NotFoundException("Lecture not found: " + lectureId);
        }
        if (!lectureRepository.existsByIdAndCourseId(lectureId, courseId)) {
            throw new BadRequestException("Lecture does not belong to the requested course");
        }
    }

    private void validateEnrollment(UUID courseId, UserEntity user) {
        if (user.getRole() == UserRole.STUDENT && !enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new BadRequestException("Student is not enrolled in this course");
        }
    }

    private UserEntity currentUser() {
        return userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
    }
}
