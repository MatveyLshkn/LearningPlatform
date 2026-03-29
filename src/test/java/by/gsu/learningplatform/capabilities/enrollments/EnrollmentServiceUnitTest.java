package by.gsu.learningplatform.capabilities.enrollments;

import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ConflictException;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.observability.BusinessMetrics;
import by.gsu.learningplatform.core.security.AppPrincipal;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceUnitTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private UserService userService;
    @Mock
    private CourseService courseService;
    @Mock
    private AuthFacade authFacade;
    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private EnrollmentService service;

    @Test
    void shouldRejectTeacherEnrollment() {
        final var userId = UUID.randomUUID();
        final var courseId = UUID.randomUUID();

        final var teacher = new UserEntity();
        teacher.setId(userId);
        teacher.setRole(UserRole.TEACHER);

        when(authFacade.currentPrincipal()).thenReturn(new AppPrincipal(userId, "sub", "teacher", Set.of("ROLE_TEACHER")));
        when(userService.getByKeycloakSub("sub")).thenReturn(teacher);

        assertThrows(ForbiddenException.class, () -> service.enroll(new EnrollmentCreateRequest(courseId)));
    }

    @Test
    void shouldRejectDuplicateEnrollment() {
        final var userId = UUID.randomUUID();
        final var courseId = UUID.randomUUID();

        final var student = new UserEntity();
        student.setId(userId);
        student.setRole(UserRole.STUDENT);

        when(authFacade.currentPrincipal()).thenReturn(new AppPrincipal(userId, "sub", "student", Set.of("ROLE_STUDENT")));
        when(userService.getByKeycloakSub("sub")).thenReturn(student);
        when(enrollmentRepository.findByUserIdAndCourseId(userId, courseId)).thenReturn(Optional.of(new EnrollmentEntity()));

        assertThrows(ConflictException.class, () -> service.enroll(new EnrollmentCreateRequest(courseId)));
    }
}
