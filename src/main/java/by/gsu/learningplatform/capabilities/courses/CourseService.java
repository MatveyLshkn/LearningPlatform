package by.gsu.learningplatform.capabilities.courses;

import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final AuthFacade authFacade;
    private final UserService userService;

    public CourseService(CourseRepository courseRepository,
                         CourseMapper courseMapper,
                         AuthFacade authFacade,
                         UserService userService) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.authFacade = authFacade;
        this.userService = userService;
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.STUDENT) {
            throw new ForbiddenException("Students cannot create courses");
        }

        final var entity = new CourseEntity();
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setTeacherId(actor.getId());

        return courseMapper.toResponse(courseRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> findAll(Pageable pageable) {
        return courseRepository.findAll(pageable).map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseEntity getEntity(UUID courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
    }

    @Transactional
    public CourseResponse update(UUID courseId, CourseRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = getEntity(courseId);
        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(course.getTeacherId())) {
            throw new ForbiddenException("You can update only your own course");
        }

        course.setTitle(request.title());
        course.setDescription(request.description());
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public void delete(UUID courseId) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = getEntity(courseId);

        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(course.getTeacherId())) {
            throw new ForbiddenException("You can delete only your own course");
        }

        courseRepository.delete(course);
    }
}
