package by.gsu.learningplatform.capabilities.lessons;

import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.lectures.LectureRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.BadRequestException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;
    private final UserService userService;
    private final AuthFacade authFacade;

    public LessonService(LessonRepository lessonRepository,
                         LessonMapper lessonMapper,
                         CourseService courseService,
                         EnrollmentRepository enrollmentRepository,
                         LectureRepository lectureRepository,
                         UserService userService,
                         AuthFacade authFacade) {
        this.lessonRepository = lessonRepository;
        this.lessonMapper = lessonMapper;
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.lectureRepository = lectureRepository;
        this.userService = userService;
        this.authFacade = authFacade;
    }

    @Transactional
    public LessonResponse create(LessonRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(request.courseId());

        final var canManage = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canManage) {
            throw new ForbiddenException("Only course teacher or admin can create lessons");
        }

        final var lesson = new LessonEntity();
        lesson.setCourseId(request.courseId());
        lesson.setTitle(request.title());
        lesson.setContent(request.content());

        return lessonMapper.toResponse(lessonRepository.save(lesson));
    }

    @Transactional(readOnly = true)
    public LessonResponse getById(UUID lessonId) {
        final var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found: " + lessonId));
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(lesson.getCourseId());
        validateCanReadContent(actor, course);
        return lessonMapper.toResponse(lesson);
    }

    @Transactional(readOnly = true)
    public Page<LessonResponse> listByCourse(UUID courseId, Pageable pageable) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(courseId);
        validateCanReadContent(actor, course);
        return lessonRepository.findByCourseId(courseId, pageable).map(lessonMapper::toResponse);
    }

    @Transactional
    public LessonResponse update(UUID lessonId, LessonUpdateRequest request) {
        final var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found: " + lessonId));
        final var course = courseService.getEntity(lesson.getCourseId());
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        validateCanManage(actor, course);

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("Lesson title must not be blank");
            }
            lesson.setTitle(request.title());
        }
        if (request.content() != null) {
            if (request.content().isBlank()) {
                throw new BadRequestException("Lesson content must not be blank");
            }
            lesson.setContent(request.content());
        }
        return lessonMapper.toResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public void delete(UUID lessonId) {
        final var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found: " + lessonId));
        final var course = courseService.getEntity(lesson.getCourseId());
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        validateCanManage(actor, course);
        lectureRepository.deleteByLessonId(lessonId);
        lessonRepository.delete(lesson);
    }

    private void validateCanReadContent(UserEntity actor, CourseEntity course) {
        if (actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId())) {
            return;
        }
        if (actor.getRole() == UserRole.STUDENT && enrollmentRepository.existsByUserIdAndCourseId(actor.getId(), course.getId())) {
            return;
        }
        throw new ForbiddenException("Only course teacher, enrolled students, or admin can view course content");
    }

    private void validateCanManage(UserEntity actor, CourseEntity course) {
        if (actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId())) {
            return;
        }
        throw new ForbiddenException("Only course teacher or admin can manage lessons");
    }
}
