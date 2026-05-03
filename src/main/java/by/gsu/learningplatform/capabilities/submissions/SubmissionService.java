package by.gsu.learningplatform.capabilities.submissions;

import lombok.val;
import by.gsu.learningplatform.capabilities.assessments.AssessmentEntity;
import by.gsu.learningplatform.capabilities.assessments.AssessmentService;
import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ConflictException;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.observability.BusinessMetrics;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;
    private final AssessmentService assessmentService;
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;
    private final AuthFacade authFacade;
    private final BusinessMetrics businessMetrics;

    public SubmissionService(SubmissionRepository submissionRepository,
                             SubmissionMapper submissionMapper,
                             AssessmentService assessmentService,
                             CourseService courseService,
                             EnrollmentRepository enrollmentRepository,
                             UserService userService,
                             AuthFacade authFacade,
                             BusinessMetrics businessMetrics) {
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
        this.assessmentService = assessmentService;
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.userService = userService;
        this.authFacade = authFacade;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public SubmissionResponse submit(final SubmissionCreateRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        if (actor.getRole() == UserRole.TEACHER) {
            throw new ForbiddenException("Teachers cannot submit student answers");
        }

        val assessment = assessmentService.getEntity(request.assessmentId());
        courseService.getEntity(assessment.getCourseId());
        if (actor.getRole() == UserRole.STUDENT && !enrollmentRepository.existsByUserIdAndCourseId(actor.getId(), assessment.getCourseId())) {
            throw new ForbiddenException("Only enrolled students can submit answers for this course");
        }
        if (submissionRepository.existsByAssessmentIdAndStudentId(request.assessmentId(), actor.getId())) {
            throw new ConflictException("Assessment already has a submission from this user");
        }

        val entity = new SubmissionEntity();
        entity.setAssessmentId(request.assessmentId());
        entity.setStudentId(actor.getId());
        entity.setAnswerText(request.answerText());

        val saved = submissionRepository.saveAndFlush(entity);
        businessMetrics.incrementSubmissions();
        return submissionMapper.toResponse(saved);
    }

    @Transactional
    public SubmissionResponse grade(final UUID submissionId, final SubmissionGradeRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        val assessment = assessmentService.getEntity(submission.getAssessmentId());
        val course = courseService.getEntity(assessment.getCourseId());

        val canGrade = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canGrade) {
            throw new ForbiddenException("Only course teacher or admin can grade submissions");
        }

        submission.setScore(request.score());
        submission.setGradedAt(OffsetDateTime.now());
        val saved = submissionRepository.save(submission);
        businessMetrics.incrementGradedSubmissions();
        return submissionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> listOwn(final Pageable pageable) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        return submissionRepository.findByStudentId(actor.getId(), pageable).map(submissionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> listByAssessment(final UUID assessmentId, final Pageable pageable) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val assessment = assessmentService.getEntity(assessmentId);
        val course = courseService.getEntity(assessment.getCourseId());
        val canRead = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canRead) {
            throw new ForbiddenException("Only course teacher or admin can view assessment submissions");
        }
        return submissionRepository.findByAssessmentId(assessmentId, pageable).map(submissionMapper::toResponse);
    }
}
