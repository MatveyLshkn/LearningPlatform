package by.gsu.learningplatform.capabilities.lectures;

import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.lessons.LessonEntity;
import by.gsu.learningplatform.capabilities.lessons.LessonRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.BadRequestException;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
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
    private final UserService userService;
    private final AuthFacade authFacade;

    public LectureService(LectureRepository lectureRepository,
                          LectureMapper lectureMapper,
                          LessonRepository lessonRepository,
                          CourseService courseService,
                          UserService userService,
                          AuthFacade authFacade) {
        this.lectureRepository = lectureRepository;
        this.lectureMapper = lectureMapper;
        this.lessonRepository = lessonRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.authFacade = authFacade;
    }

    @Transactional
    public LectureResponse create(LectureRequest request) {
        if ((request.videoUrl() == null || request.videoUrl().isBlank()) &&
                (request.content() == null || request.content().isBlank())) {
            throw new BadRequestException("Either videoUrl or content must be provided");
        }

        final var lesson = lessonRepository.findById(request.lessonId())
                .orElseThrow(() -> new NotFoundException("Lesson not found: " + request.lessonId()));

        final var course = courseService.getEntity(lesson.getCourseId());
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());

        final var canManage = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canManage) {
            throw new ForbiddenException("Only course teacher or admin can create lectures");
        }

        final var lecture = new LectureEntity();
        lecture.setLessonId(request.lessonId());
        lecture.setTitle(request.title());
        lecture.setVideoUrl(request.videoUrl());
        lecture.setContent(request.content());

        return lectureMapper.toResponse(lectureRepository.save(lecture));
    }

    @Transactional(readOnly = true)
    public List<LectureResponse> listByLesson(UUID lessonId) {
        return lectureRepository.findByLessonId(lessonId).stream().map(lectureMapper::toResponse).toList();
    }
}
