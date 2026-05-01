package by.gsu.learningplatform.capabilities.assessments;

import lombok.val;
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
    public AssessmentResponse create(final AssessmentCreateRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = courseService.getEntity(request.courseId());

        validateTeacherOrAdminAccess(actor, course);
        if (request.lessonId() != null && request.lectureId() != null) {
            throw new BadRequestException("Use lessonId or lectureId, not both");
        }
        validateAssessmentPayload(request.questions(), request.answerKey(), request.rubricCriteria());

        val entity = new AssessmentEntity();
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
    public AssessmentDraftResponse generateDraft(final AssessmentGenerateRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = courseService.getEntity(request.courseId());
        validateTeacherOrAdminAccess(actor, course);
        validateSourceScope(request);

        val source = buildSourceMaterial(request, course);
        val requestedCount = request.questionCount() == null ? 6 : request.questionCount();
        val difficulty = request.difficulty() == null || request.difficulty().isBlank() ? "medium" : request.difficulty();

        AiAssessmentDraftPayload payload;
        try {
            val response = aiClientService.generate(
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
    public AssessmentResponse createFromDraft(final AssessmentCreateFromDraftRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = courseService.getEntity(request.courseId());
        validateTeacherOrAdminAccess(actor, course);
        validateDraftSourceScope(request);
        validateAssessmentPayload(request.questions(), request.answerKey(), request.rubricCriteria());

        val entity = new AssessmentEntity();
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
    public AssessmentStudentResponse getStudentView(final UUID assessmentId) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val assessment = getEntity(assessmentId);
        val course = courseService.getEntity(assessment.getCourseId());
        validateCanReadAssessment(actor, course);
        return toStudentResponse(assessment);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getDetails(final UUID assessmentId) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val assessment = getEntity(assessmentId);
        val course = courseService.getEntity(assessment.getCourseId());
        validateTeacherOrAdminAccess(actor, course);
        return assessmentMapper.toResponse(assessment);
    }

    @Transactional(readOnly = true)
    public AssessmentEntity getEntity(final UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NotFoundException("Assessment not found: " + assessmentId));
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<AssessmentStudentResponse> listByCourse(final UUID courseId, final Integer limit, final String cursor) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = courseService.getEntity(courseId);
        validateCanReadAssessment(actor, course);
        val pageable = paginationUtils.toPageable(limit, cursor);
        final Page<AssessmentStudentResponse> page = assessmentRepository.findByCourseId(courseId, pageable).map(this::toStudentResponse);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/courses/" + courseId + "/assessments?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }

    @Transactional
    public AssessmentResponse update(final UUID assessmentId, final AssessmentUpdateRequest request) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val assessment = getEntity(assessmentId);
        val course = courseService.getEntity(assessment.getCourseId());
        validateTeacherOrAdminAccess(actor, course);

        val nextQuestions = request.questions() == null ? readStringList(assessment.getQuestionsJson()) : request.questions();
        val nextAnswerKey = request.answerKey() == null ? readStringList(assessment.getAnswerKeyJson()) : request.answerKey();
        val nextRubric = request.rubricCriteria() == null ? readStringList(assessment.getRubricJson()) : request.rubricCriteria();
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
    public void delete(final UUID assessmentId) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val assessment = getEntity(assessmentId);
        val course = courseService.getEntity(assessment.getCourseId());
        validateTeacherOrAdminAccess(actor, course);
        assessmentRepository.delete(assessment);
    }

    private void validateTeacherOrAdminAccess(final UserEntity actor, final CourseEntity course) {
        val canCreate = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canCreate) {
            throw new ForbiddenException("Only course teacher or admin can manage assessments");
        }
    }

    private void validateCanReadAssessment(final UserEntity actor, final CourseEntity course) {
        if (actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId())) {
            return;
        }
        if (actor.getRole() == UserRole.STUDENT && enrollmentRepository.existsByUserIdAndCourseId(actor.getId(), course.getId())) {
            return;
        }
        throw new ForbiddenException("Only course teacher, enrolled students, or admin can view assessments");
    }

    private void validateSourceScope(final AssessmentGenerateRequest request) {
        if (request.lessonId() != null && request.lectureId() != null) {
            throw new BadRequestException("Use lessonId or lectureId, not both");
        }
    }

    private void validateDraftSourceScope(final AssessmentCreateFromDraftRequest request) {
        if (request.lessonId() != null && request.lectureId() != null) {
            throw new BadRequestException("Use lessonId or lectureId, not both");
        }
    }

    private void validateAssessmentPayload(final List<String> questions, final List<String> answerKey, final List<String> rubricCriteria) {
        if (questions.size() != answerKey.size() || questions.size() != rubricCriteria.size()) {
            throw new BadRequestException("Questions, answerKey, and rubricCriteria must have the same size");
        }
    }

    private void applySource(final AssessmentEntity entity, final UUID courseId, final UUID lessonId, final UUID lectureId) {
        if (lectureId != null) {
            val lecture = lectureRepository.findById(lectureId)
                    .orElseThrow(() -> new NotFoundException("Lecture not found: " + lectureId));
            val lesson = lessonRepository.findById(lecture.getLessonId())
                    .orElseThrow(() -> new NotFoundException("Lesson not found for lecture: " + lectureId));
            if (!lesson.getCourseId().equals(courseId)) {
                throw new BadRequestException("Lecture does not belong to the requested course");
            }
            entity.setSourceType("LECTURE");
            entity.setSourceId(lectureId);
            return;
        }
        if (lessonId != null) {
            val lesson = lessonRepository.findById(lessonId)
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

    private String buildSourceMaterial(final AssessmentGenerateRequest request, final CourseEntity course) {
        val builder = new StringBuilder();
        builder.append("Course: ").append(course.getTitle()).append('\n');
        if (course.getDescription() != null) {
            builder.append("Course description: ").append(course.getDescription()).append('\n');
        }

        if (request.lectureId() != null) {
            val lecture = lectureRepository.findById(request.lectureId())
                    .orElseThrow(() -> new NotFoundException("Lecture not found: " + request.lectureId()));
            val lesson = lessonRepository.findById(lecture.getLessonId())
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
            val lesson = lessonRepository.findById(request.lessonId())
                    .orElseThrow(() -> new NotFoundException("Lesson not found: " + request.lessonId()));
            if (!lesson.getCourseId().equals(request.courseId())) {
                throw new BadRequestException("Lesson does not belong to the requested course");
            }
            builder.append("Lesson title: ").append(lesson.getTitle()).append('\n');
            builder.append("Lesson content: ").append(lesson.getContent()).append('\n');
            return builder.toString();
        }

        val lessons = lessonRepository.findByCourseId(request.courseId());
        for (val lesson : lessons) {
            builder.append("Lesson title: ").append(lesson.getTitle()).append('\n');
            builder.append("Lesson content: ").append(lesson.getContent()).append('\n');
            val lectures = lectureRepository.findByLessonId(lesson.getId());
            for (val lecture : lectures) {
                builder.append("Lecture title: ").append(lecture.getTitle()).append('\n');
                if (lecture.getContent() != null) {
                    builder.append("Lecture content: ").append(lecture.getContent()).append('\n');
                }
            }
        }
        return builder.toString();
    }

    private String writeJson(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Cannot serialize assessment draft");
        }
    }

    private AssessmentStudentResponse toStudentResponse(final AssessmentEntity entity) {
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

    private List<String> readStringList(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Cannot parse assessment questions");
        }
    }

    private AiAssessmentDraftPayload fallbackDraftPayload(final String courseTitle, final int questionCount, final String difficulty, final String source) {
        val questions = new ArrayList<String>();
        val answerKey = new ArrayList<String>();
        val rubric = List.of(
                "Correctness of key concept explanation",
                "Use of course terminology and examples",
                "Clarity and structure of the answer"
        );
        val safeCount = Math.max(3, questionCount);
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
