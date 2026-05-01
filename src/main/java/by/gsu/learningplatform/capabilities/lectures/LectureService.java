package by.gsu.learningplatform.capabilities.lectures;

import lombok.val;
import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.lessons.LessonEntity;
import by.gsu.learningplatform.capabilities.lessons.LessonRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.BadRequestException;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LectureService {

    private final LectureRepository lectureRepository;
    private final LectureMapper lectureMapper;
    private final LessonRepository lessonRepository;
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;
    private final AuthFacade authFacade;

    public LectureService(LectureRepository lectureRepository,
                          LectureMapper lectureMapper,
                          LessonRepository lessonRepository,
                          CourseService courseService,
                          EnrollmentRepository enrollmentRepository,
                          UserService userService,
                          AuthFacade authFacade) {
        this.lectureRepository = lectureRepository;
        this.lectureMapper = lectureMapper;
        this.lessonRepository = lessonRepository;
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.userService = userService;
        this.authFacade = authFacade;
    }

    @Transactional
    public LectureResponse create(final LectureRequest request) {
        if ((request.videoUrl() == null || request.videoUrl().isBlank()) &&
                (request.content() == null || request.content().isBlank())) {
            throw new BadRequestException("Either videoUrl or content must be provided");
        }

        val lesson = lessonRepository.findById(request.lessonId())
                .orElseThrow(() -> new NotFoundException("Lesson not found: " + request.lessonId()));

        val course = courseService.getEntity(lesson.getCourseId());
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());

        val canManage = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canManage) {
            throw new ForbiddenException("Only course teacher or admin can create lectures");
        }

        val lecture = new LectureEntity();
        lecture.setLessonId(request.lessonId());
        lecture.setTitle(request.title());
        lecture.setVideoUrl(request.videoUrl());
        lecture.setContent(request.content());

        return lectureMapper.toResponse(lectureRepository.save(lecture));
    }

    @Transactional(readOnly = true)
    public LectureResponse getById(final UUID lectureId) {
        val lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("Lecture not found: " + lectureId));
        val lesson = lessonRepository.findById(lecture.getLessonId())
                .orElseThrow(() -> new NotFoundException("Lesson not found for lecture: " + lectureId));
        val course = courseService.getEntity(lesson.getCourseId());
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        validateCanReadContent(actor, course);
        return lectureMapper.toResponse(lecture);
    }

    @Transactional(readOnly = true)
    public Page<LectureResponse> listByLesson(final UUID lessonId, final Pageable pageable) {
        val lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lesson not found: " + lessonId));
        val course = courseService.getEntity(lesson.getCourseId());
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        validateCanReadContent(actor, course);
        return lectureRepository.findByLessonId(lessonId, pageable).map(lectureMapper::toResponse);
    }

    @Transactional
    public LectureResponse update(final UUID lectureId, final LectureUpdateRequest request) {
        val lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("Lecture not found: " + lectureId));
        val lesson = lessonRepository.findById(lecture.getLessonId())
                .orElseThrow(() -> new NotFoundException("Lesson not found for lecture: " + lectureId));
        val course = courseService.getEntity(lesson.getCourseId());
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        validateCanManage(actor, course);

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("Lecture title must not be blank");
            }
            lecture.setTitle(request.title());
        }
        if (request.videoUrl() != null) {
            lecture.setVideoUrl(request.videoUrl().isBlank() ? null : request.videoUrl());
        }
        if (request.content() != null) {
            lecture.setContent(request.content().isBlank() ? null : request.content());
        }
        if ((lecture.getVideoUrl() == null || lecture.getVideoUrl().isBlank()) &&
                (lecture.getContent() == null || lecture.getContent().isBlank())) {
            throw new BadRequestException("Either videoUrl or content must be provided");
        }
        return lectureMapper.toResponse(lectureRepository.save(lecture));
    }

    @Transactional
    public void delete(final UUID lectureId) {
        val lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new NotFoundException("Lecture not found: " + lectureId));
        val lesson = lessonRepository.findById(lecture.getLessonId())
                .orElseThrow(() -> new NotFoundException("Lesson not found for lecture: " + lectureId));
        val course = courseService.getEntity(lesson.getCourseId());
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        validateCanManage(actor, course);
        lectureRepository.delete(lecture);
    }

    private void validateCanReadContent(final UserEntity actor, final CourseEntity course) {
        if (actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId())) {
            return;
        }
        if (actor.getRole() == UserRole.STUDENT && enrollmentRepository.existsByUserIdAndCourseId(actor.getId(), course.getId())) {
            return;
        }
        throw new ForbiddenException("Only course teacher, enrolled students, or admin can view course content");
    }

    private void validateCanManage(final UserEntity actor, final CourseEntity course) {
        if (actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId())) {
            return;
        }
        throw new ForbiddenException("Only course teacher or admin can manage lectures");
    }
}
