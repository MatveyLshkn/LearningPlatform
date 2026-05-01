package by.gsu.learningplatform.capabilities.submissions;

import by.gsu.learningplatform.capabilities.assessments.AssessmentService;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.observability.BusinessMetrics;
import by.gsu.learningplatform.core.security.AppPrincipal;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceUnitTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private CourseService courseService;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserService userService;
    @Mock
    private AuthFacade authFacade;
    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private SubmissionService service;

    @Test
    void shouldRejectTeacherSubmission() {
        final var userId = UUID.randomUUID();
        final var teacher = new UserEntity();
        teacher.setId(userId);
        teacher.setRole(UserRole.TEACHER);

        when(authFacade.currentPrincipal()).thenReturn(new AppPrincipal(userId, "sub", "teacher", Set.of("ROLE_TEACHER")));
        when(userService.getByKeycloakSub("sub")).thenReturn(teacher);

        assertThrows(ForbiddenException.class,
                () -> service.submit(new SubmissionCreateRequest(UUID.randomUUID(), "answer")));
    }
}
