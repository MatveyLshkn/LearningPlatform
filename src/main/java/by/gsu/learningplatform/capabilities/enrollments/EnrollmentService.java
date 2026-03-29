package by.gsu.learningplatform.capabilities.enrollments;

import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ConflictException;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.observability.BusinessMetrics;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final UserService userService;
    private final CourseService courseService;
    private final AuthFacade authFacade;
    private final BusinessMetrics businessMetrics;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             EnrollmentMapper enrollmentMapper,
                             UserService userService,
                             CourseService courseService,
                             AuthFacade authFacade,
                             BusinessMetrics businessMetrics) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.userService = userService;
        this.courseService = courseService;
        this.authFacade = authFacade;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public EnrollmentResponse enroll(EnrollmentCreateRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.TEACHER) {
            throw new ForbiddenException("Teachers cannot enroll as students");
        }

        courseService.getEntity(request.courseId());
        enrollmentRepository.findByUserIdAndCourseId(actor.getId(), request.courseId()).ifPresent(existing -> {
            throw new ConflictException("User already enrolled in this course");
        });

        final var entity = new EnrollmentEntity();
        entity.setUserId(actor.getId());
        entity.setCourseId(request.courseId());

        final var saved = enrollmentRepository.save(entity);
        businessMetrics.incrementEnrollments();
        return enrollmentMapper.toResponse(saved);
    }

    @Transactional
    public void leave(UUID enrollmentId) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found: " + enrollmentId));

        final var isAdmin = actor.getRole() == UserRole.ADMIN;
        final var isOwner = actor.getId().equals(enrollment.getUserId());
        if (!(isAdmin || isOwner)) {
            throw new ForbiddenException("You can leave only your own enrollment");
        }

        enrollmentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listOwn() {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        return enrollmentRepository.findByUserId(actor.getId()).stream().map(enrollmentMapper::toResponse).toList();
    }
}
