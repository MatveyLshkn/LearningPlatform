package by.gsu.learningplatform.capabilities.users;

import by.gsu.learningplatform.capabilities.assessments.AssessmentMapper;
import by.gsu.learningplatform.capabilities.assessments.AssessmentRepository;
import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseMapper;
import by.gsu.learningplatform.capabilities.courses.CourseRepository;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentMapper;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.submissions.SubmissionMapper;
import by.gsu.learningplatform.capabilities.submissions.SubmissionRepository;
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
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final SubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentMapper assessmentMapper;
    private final AuthFacade authFacade;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       EnrollmentRepository enrollmentRepository,
                       EnrollmentMapper enrollmentMapper,
                       CourseRepository courseRepository,
                       CourseMapper courseMapper,
                       SubmissionRepository submissionRepository,
                       SubmissionMapper submissionMapper,
                       AssessmentRepository assessmentRepository,
                       AssessmentMapper assessmentMapper,
                       AuthFacade authFacade) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
        this.assessmentRepository = assessmentRepository;
        this.assessmentMapper = assessmentMapper;
        this.authFacade = authFacade;
    }

    public UserEntity getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public UserEntity getByKeycloakSub(String keycloakSub) {
        return userRepository.findByKeycloakSub(keycloakSub)
                .orElseThrow(() -> new NotFoundException("User not found for keycloak subject"));
    }

    public UserResponse getUserResponse(UUID id) {
        return userMapper.toResponse(getById(id));
    }

    public UserResponse getVisibleUserResponse(UUID id) {
        final var actor = getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(id)) {
            throw new ForbiddenException("You can view only your own profile");
        }
        return userMapper.toResponse(getById(id));
    }

    public Page<UserResponse> listUsers(UserRole role, Pageable pageable) {
        final var page = role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable);
        return page.map(userMapper::toResponse);
    }

    public UserDetailsResponse getUserDetails(UUID userId) {
        final var user = getById(userId);
        final var enrollments = enrollmentRepository.findByUserId(userId);
        final var courseIds = enrollments.stream().map(enrollment -> enrollment.getCourseId()).toList();
        final List<CourseEntity> enrolledCourses = courseIds.isEmpty() ? List.of() : courseRepository.findByIdIn(courseIds);
        final var taughtCourses = courseRepository.findByTeacherId(userId);
        final var submissions = submissionRepository.findByStudentId(userId);
        final var createdAssessments = assessmentRepository.findByCreatedBy(userId);

        return new UserDetailsResponse(
                userMapper.toResponse(user),
                enrollments.stream().map(enrollmentMapper::toResponse).toList(),
                enrolledCourses.stream().map(courseMapper::toResponse).toList(),
                taughtCourses.stream().map(courseMapper::toResponse).toList(),
                submissions.stream().map(submissionMapper::toResponse).toList(),
                createdAssessments.stream().map(assessmentMapper::toResponse).toList());
    }
}
