package by.gsu.learningplatform.capabilities.lessons;

import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthFacade authFacade;

    public LessonService(LessonRepository lessonRepository,
                         LessonMapper lessonMapper,
                         CourseService courseService,
                         UserService userService,
                         AuthFacade authFacade) {
        this.lessonRepository = lessonRepository;
        this.lessonMapper = lessonMapper;
        this.courseService = courseService;
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
    public List<LessonResponse> listByCourse(UUID courseId) {
        return lessonRepository.findByCourseId(courseId).stream().map(lessonMapper::toResponse).toList();
    }
}
