package by.gsu.learningplatform.capabilities.enrollments;

import lombok.val;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserMapper;
import by.gsu.learningplatform.capabilities.users.UserRepository;
import by.gsu.learningplatform.capabilities.users.UserResponse;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ConflictException;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.observability.BusinessMetrics;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserService userService;
    private final CourseService courseService;
    private final AuthFacade authFacade;
    private final BusinessMetrics businessMetrics;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             EnrollmentMapper enrollmentMapper,
                             UserRepository userRepository,
                             UserMapper userMapper,
                             UserService userService,
                             CourseService courseService,
                             AuthFacade authFacade,
                             BusinessMetrics businessMetrics) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userService = userService;
        this.courseService = courseService;
        this.authFacade = authFacade;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public EnrollmentResponse enroll(final EnrollmentCreateRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.TEACHER) {
            throw new ForbiddenException("Teachers cannot enroll as students");
        }

        courseService.getEntity(request.courseId());
        enrollmentRepository.findByUserIdAndCourseId(actor.getId(), request.courseId()).ifPresent(existing -> {
            throw new ConflictException("User already enrolled in this course");
        });

        val entity = new EnrollmentEntity();
        entity.setUserId(actor.getId());
        entity.setCourseId(request.courseId());

        val saved = enrollmentRepository.saveAndFlush(entity);
        businessMetrics.incrementEnrollments();
        return enrollmentMapper.toResponse(saved);
    }

    @Transactional
    public void leave(final UUID enrollmentId) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found: " + enrollmentId));

        val isAdmin = actor.getRole() == UserRole.ADMIN;
        val isOwner = actor.getId().equals(enrollment.getUserId());
        if (!(isAdmin || isOwner)) {
            throw new ForbiddenException("You can leave only your own enrollment");
        }

        enrollmentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> listOwn(final Pageable pageable) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        return enrollmentRepository.findByUserId(actor.getId(), pageable).map(enrollmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listCourseStudents(final UUID courseId, final Pageable pageable) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = courseService.getEntity(courseId);
        val canReadRoster = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canReadRoster) {
            throw new ForbiddenException("Only course teacher or admin can view course students");
        }

        val enrollmentPage = enrollmentRepository.findByCourseId(courseId, pageable);
        val studentIds = enrollmentPage.stream().map(EnrollmentEntity::getUserId).toList();
        if (studentIds.isEmpty()) {
            return Page.empty(pageable);
        }
        final Map<UUID, UserEntity> studentsById = userRepository.findByIdInAndRole(studentIds, UserRole.STUDENT).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        val students = enrollmentPage.stream()
                .map(enrollment -> studentsById.get(enrollment.getUserId()))
                .filter(java.util.Objects::nonNull)
                .map(userMapper::toResponse)
                .toList();
        return new PageImpl<>(students, pageable, enrollmentPage.getTotalElements());
    }
}
