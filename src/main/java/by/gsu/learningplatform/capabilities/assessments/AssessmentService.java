package by.gsu.learningplatform.capabilities.assessments;

import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMapper assessmentMapper;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthFacade authFacade;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentMapper assessmentMapper,
                             CourseService courseService,
                             UserService userService,
                             AuthFacade authFacade) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMapper = assessmentMapper;
        this.courseService = courseService;
        this.userService = userService;
        this.authFacade = authFacade;
    }

    @Transactional
    public AssessmentResponse create(AssessmentCreateRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(request.courseId());

        final var canCreate = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canCreate) {
            throw new ForbiddenException("Only course teacher or admin can create assessments");
        }

        final var entity = new AssessmentEntity();
        entity.setCourseId(request.courseId());
        entity.setCreatedBy(actor.getId());

        return assessmentMapper.toResponse(assessmentRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public AssessmentEntity getEntity(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("Assessment not found: " + assessmentId));
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> listByCourse(UUID courseId) {
        return assessmentRepository.findByCourseId(courseId).stream().map(assessmentMapper::toResponse).toList();
    }
}
