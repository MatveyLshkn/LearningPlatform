package by.gsu.learningplatform.capabilities.assessments;

import by.gsu.learningplatform.capabilities.ai.AiAssessmentDraftPayload;
import by.gsu.learningplatform.capabilities.ai.AiClientService;
import by.gsu.learningplatform.capabilities.ai.AiIntegrationException;
import by.gsu.learningplatform.capabilities.ai.AiPromptBuilderService;
import by.gsu.learningplatform.capabilities.ai.AiSafetyService;
import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.lectures.LectureRepository;
import by.gsu.learningplatform.capabilities.lessons.LessonRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.BadRequestException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
import by.gsu.learningplatform.core.web.CursorPageResponse;
import by.gsu.learningplatform.core.web.PaginationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMapper assessmentMapper;
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LectureRepository lectureRepository;
    private final UserService userService;
    private final AuthFacade authFacade;
    private final AiPromptBuilderService aiPromptBuilderService;
    private final AiClientService aiClientService;
    private final AiSafetyService aiSafetyService;
    private final ObjectMapper objectMapper;
    private final PaginationUtils paginationUtils;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentMapper assessmentMapper,
                             CourseService courseService,
                             EnrollmentRepository enrollmentRepository,
                             LessonRepository lessonRepository,
                             LectureRepository lectureRepository,
                             UserService userService,
                             AuthFacade authFacade,
                             AiPromptBuilderService aiPromptBuilderService,
                             AiClientService aiClientService,
                             AiSafetyService aiSafetyService,
                             ObjectMapper objectMapper,
                             PaginationUtils paginationUtils) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMapper = assessmentMapper;
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonRepository = lessonRepository;
        this.lectureRepository = lectureRepository;
        this.userService = userService;
        this.authFacade = authFacade;
        this.aiPromptBuilderService = aiPromptBuilderService;
        this.aiClientService = aiClientService;
        this.aiSafetyService = aiSafetyService;
        this.objectMapper = objectMapper;
        this.paginationUtils = paginationUtils;
    }

    @Transactional
    public AssessmentResponse create(AssessmentCreateRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(request.courseId());

        validateTeacherOrAdminAccess(actor, course);
        if (request.lessonId() != null && request.lectureId() != null) {
            throw new BadRequestException("Use lessonId or lectureId, not both");
        }
        validateAssessmentPayload(request.questions(), request.answerKey(), request.rubricCriteria());

        final var entity = new AssessmentEntity();
        entity.setCourseId(request.courseId());
        entity.setCreatedBy(actor.getId());
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setQuestionsJson(writeJson(request.questions()));
        entity.setAnswerKeyJson(writeJson(request.answerKey()));
        entity.setRubricJson(writeJson(request.rubricCriteria()));
        applySource(entity, request.courseId(), request.lessonId(), request.lectureId());

        return assessmentMapper.toResponse(assessmentRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public AssessmentDraftResponse generateDraft(AssessmentGenerateRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(request.courseId());
        validateTeacherOrAdminAccess(actor, course);
        validateSourceScope(request);

        final var source = buildSourceMaterial(request, course);
        final var requestedCount = request.questionCount() == null ? 6 : request.questionCount();
        final var difficulty = request.difficulty() == null || request.difficulty().isBlank() ? "medium" : request.difficulty();

        AiAssessmentDraftPayload payload;
        try {
            final var response = aiClientService.generate(
                    aiPromptBuilderService.assessmentDraftSystemPrompt(),
                    aiPromptBuilderService.buildAssessmentDraftUserPrompt(source, requestedCount, difficulty));
            payload = aiSafetyService.parseAssessmentDraft(response);
        } catch (AiIntegrationException ex) {
            payload = fallbackDraftPayload(course.getTitle(), requestedCount, difficulty, source);
        }

        return new AssessmentDraftResponse(
                request.courseId(),
                request.lessonId(),
                request.lectureId(),
                payload.title(),
                payload.description(),
                payload.questions(),
                payload.answerKey(),
                payload.rubricCriteria()
        );
    }

    @Transactional
    public AssessmentResponse createFromDraft(AssessmentCreateFromDraftRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(request.courseId());
        validateTeacherOrAdminAccess(actor, course);
        validateDraftSourceScope(request);
        validateAssessmentPayload(request.questions(), request.answerKey(), request.rubricCriteria());

        final var entity = new AssessmentEntity();
        entity.setCourseId(request.courseId());
        entity.setCreatedBy(actor.getId());
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setQuestionsJson(writeJson(request.questions()));
        entity.setAnswerKeyJson(writeJson(request.answerKey()));
        entity.setRubricJson(writeJson(request.rubricCriteria()));

        applySource(entity, request.courseId(), request.lessonId(), request.lectureId());

        return assessmentMapper.toResponse(assessmentRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public AssessmentStudentResponse getStudentView(UUID assessmentId) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var assessment = getEntity(assessmentId);
        final var course = courseService.getEntity(assessment.getCourseId());
        validateCanReadAssessment(actor, course);
        return toStudentResponse(assessment);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getDetails(UUID assessmentId) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var assessment = getEntity(assessmentId);
        final var course = courseService.getEntity(assessment.getCourseId());
        validateTeacherOrAdminAccess(actor, course);
        return assessmentMapper.toResponse(assessment);
    }

    @Transactional(readOnly = true)
    public AssessmentEntity getEntity(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("Assessment not found: " + assessmentId));
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<AssessmentStudentResponse> listByCourse(UUID courseId, Integer limit, String cursor) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(courseId);
        validateCanReadAssessment(actor, course);
        final var pageable = paginationUtils.toPageable(limit, cursor);
        final Page<AssessmentStudentResponse> page = assessmentRepository.findByCourseId(courseId, pageable).map(this::toStudentResponse);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/courses/" + courseId + "/assessments?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }

    @Transactional
    public AssessmentResponse update(UUID assessmentId, AssessmentUpdateRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var assessment = getEntity(assessmentId);
        final var course = courseService.getEntity(assessment.getCourseId());
        validateTeacherOrAdminAccess(actor, course);

        final var nextQuestions = request.questions() == null ? readStringList(assessment.getQuestionsJson()) : request.questions();
        final var nextAnswerKey = request.answerKey() == null ? readStringList(assessment.getAnswerKeyJson()) : request.answerKey();
        final var nextRubric = request.rubricCriteria() == null ? readStringList(assessment.getRubricJson()) : request.rubricCriteria();
        validateAssessmentPayload(nextQuestions, nextAnswerKey, nextRubric);

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("Assessment title must not be blank");
            }
            assessment.setTitle(request.title());
        }
        if (request.description() != null) {
            if (request.description().isBlank()) {
                throw new BadRequestException("Assessment description must not be blank");
            }
            assessment.setDescription(request.description());
        }
        if (request.questions() != null) {
            assessment.setQuestionsJson(writeJson(request.questions()));
        }
        if (request.answerKey() != null) {
            assessment.setAnswerKeyJson(writeJson(request.answerKey()));
        }
        if (request.rubricCriteria() != null) {
            assessment.setRubricJson(writeJson(request.rubricCriteria()));
        }
        return assessmentMapper.toResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public void delete(UUID assessmentId) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var assessment = getEntity(assessmentId);
        final var course = courseService.getEntity(assessment.getCourseId());
        validateTeacherOrAdminAccess(actor, course);
        assessmentRepository.delete(assessment);
    }

    private void validateTeacherOrAdminAccess(UserEntity actor, CourseEntity course) {
        final var canCreate = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canCreate) {
            throw new ForbiddenException("Only course teacher or admin can manage assessments");
        }
    }

    private void validateCanReadAssessment(UserEntity actor, CourseEntity course) {
        if (actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId())) {
            return;
        }
        if (actor.getRole() == UserRole.STUDENT && enrollmentRepository.existsByUserIdAndCourseId(actor.getId(), course.getId())) {
            return;
        }
        throw new ForbiddenException("Only course teacher, enrolled students, or admin can view assessments");
    }

    private void validateSourceScope(AssessmentGenerateRequest request) {
        if (request.lessonId() != null && request.lectureId() != null) {
            throw new BadRequestException("Use lessonId or lectureId, not both");
        }
    }

    private void validateDraftSourceScope(AssessmentCreateFromDraftRequest request) {
        if (request.lessonId() != null && request.lectureId() != null) {
            throw new BadRequestException("Use lessonId or lectureId, not both");
        }
    }

    private void validateAssessmentPayload(List<String> questions, List<String> answerKey, List<String> rubricCriteria) {
        if (questions.size() != answerKey.size() || questions.size() != rubricCriteria.size()) {
            throw new BadRequestException("Questions, answerKey, and rubricCriteria must have the same size");
        }
    }

    private void applySource(AssessmentEntity entity, UUID courseId, UUID lessonId, UUID lectureId) {
        if (lectureId != null) {
            final var lecture = lectureRepository.findById(lectureId)
                    .orElseThrow(() -> new NotFoundException("Lecture not found: " + lectureId));
            final var lesson = lessonRepository.findById(lecture.getLessonId())
                    .orElseThrow(() -> new NotFoundException("Lesson not found for lecture: " + lectureId));
            if (!lesson.getCourseId().equals(courseId)) {
                throw new BadRequestException("Lecture does not belong to the requested course");
            }
            entity.setSourceType("LECTURE");
            entity.setSourceId(lectureId);
            return;
        }
        if (lessonId != null) {
            final var lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new NotFoundException("Lesson not found: " + lessonId));
            if (!lesson.getCourseId().equals(courseId)) {
                throw new BadRequestException("Lesson does not belong to the requested course");
            }
            entity.setSourceType("LESSON");
            entity.setSourceId(lessonId);
            return;
        }
        entity.setSourceType("COURSE");
        entity.setSourceId(courseId);
    }

    private String buildSourceMaterial(AssessmentGenerateRequest request, CourseEntity course) {
        final var builder = new StringBuilder();
        builder.append("Course: ").append(course.getTitle()).append('\n');
        if (course.getDescription() != null) {
            builder.append("Course description: ").append(course.getDescription()).append('\n');
        }

        if (request.lectureId() != null) {
            final var lecture = lectureRepository.findById(request.lectureId())
                    .orElseThrow(() -> new NotFoundException("Lecture not found: " + request.lectureId()));
            final var lesson = lessonRepository.findById(lecture.getLessonId())
                    .orElseThrow(() -> new NotFoundException("Lesson not found for lecture: " + request.lectureId()));
            if (!lesson.getCourseId().equals(request.courseId())) {
                throw new BadRequestException("Lecture does not belong to the requested course");
            }
            builder.append("Lecture title: ").append(lecture.getTitle()).append('\n');
            if (lecture.getContent() != null) {
                builder.append("Lecture content: ").append(lecture.getContent()).append('\n');
            }
            if (lecture.getVideoUrl() != null) {
                builder.append("Lecture video URL: ").append(lecture.getVideoUrl()).append('\n');
            }
            return builder.toString();
        }

        if (request.lessonId() != null) {
            final var lesson = lessonRepository.findById(request.lessonId())
                    .orElseThrow(() -> new NotFoundException("Lesson not found: " + request.lessonId()));
            if (!lesson.getCourseId().equals(request.courseId())) {
                throw new BadRequestException("Lesson does not belong to the requested course");
            }
            builder.append("Lesson title: ").append(lesson.getTitle()).append('\n');
            builder.append("Lesson content: ").append(lesson.getContent()).append('\n');
            return builder.toString();
        }

        final var lessons = lessonRepository.findByCourseId(request.courseId());
        for (var lesson : lessons) {
            builder.append("Lesson title: ").append(lesson.getTitle()).append('\n');
            builder.append("Lesson content: ").append(lesson.getContent()).append('\n');
            final var lectures = lectureRepository.findByLessonId(lesson.getId());
            for (var lecture : lectures) {
                builder.append("Lecture title: ").append(lecture.getTitle()).append('\n');
                if (lecture.getContent() != null) {
                    builder.append("Lecture content: ").append(lecture.getContent()).append('\n');
                }
            }
        }
        return builder.toString();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Cannot serialize assessment draft");
        }
    }

    private AssessmentStudentResponse toStudentResponse(AssessmentEntity entity) {
        return new AssessmentStudentResponse(
                entity.getId(),
                entity.getCourseId(),
                entity.getTitle(),
                entity.getDescription(),
                readStringList(entity.getQuestionsJson()),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getCreatedAt());
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Cannot parse assessment questions");
        }
    }

    private AiAssessmentDraftPayload fallbackDraftPayload(String courseTitle, int questionCount, String difficulty, String source) {
        final var questions = new ArrayList<String>();
        final var answerKey = new ArrayList<String>();
        final var rubric = List.of(
                "Correctness of key concept explanation",
                "Use of course terminology and examples",
                "Clarity and structure of the answer"
        );
        final var safeCount = Math.max(3, questionCount);
        for (int i = 1; i <= safeCount; i++) {
            questions.add("Q" + i + ". Explain one important concept from the provided material and apply it in a practical example.");
            answerKey.add("A" + i + ". The answer should define the concept, explain why it matters, and show one practical application.");
        }
        return new AiAssessmentDraftPayload(
                "Assessment Draft - " + courseTitle,
                "Fallback assessment generated because AI provider was unavailable. Difficulty: " + difficulty + ".",
                questions,
                answerKey,
                rubric
        );
    }
}
