package by.gsu.learningplatform.capabilities.assessments;

import by.gsu.learningplatform.capabilities.ai.AiAssessmentDraftPayload;
import by.gsu.learningplatform.capabilities.ai.AiClientService;
import by.gsu.learningplatform.capabilities.ai.AiIntegrationException;
import by.gsu.learningplatform.capabilities.ai.AiPromptBuilderService;
import by.gsu.learningplatform.capabilities.ai.AiSafetyService;
import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.lectures.LectureRepository;
import by.gsu.learningplatform.capabilities.lessons.LessonRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.error.BadRequestException;
import by.gsu.learningplatform.core.error.NotFoundException;
import by.gsu.learningplatform.core.security.AuthFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMapper assessmentMapper;
    private final CourseService courseService;
    private final LessonRepository lessonRepository;
    private final LectureRepository lectureRepository;
    private final UserService userService;
    private final AuthFacade authFacade;
    private final AiPromptBuilderService aiPromptBuilderService;
    private final AiClientService aiClientService;
    private final AiSafetyService aiSafetyService;
    private final ObjectMapper objectMapper;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentMapper assessmentMapper,
                             CourseService courseService,
                             LessonRepository lessonRepository,
                             LectureRepository lectureRepository,
                             UserService userService,
                             AuthFacade authFacade,
                             AiPromptBuilderService aiPromptBuilderService,
                             AiClientService aiClientService,
                             AiSafetyService aiSafetyService,
                             ObjectMapper objectMapper) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMapper = assessmentMapper;
        this.courseService = courseService;
        this.lessonRepository = lessonRepository;
        this.lectureRepository = lectureRepository;
        this.userService = userService;
        this.authFacade = authFacade;
        this.aiPromptBuilderService = aiPromptBuilderService;
        this.aiClientService = aiClientService;
        this.aiSafetyService = aiSafetyService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AssessmentResponse create(AssessmentCreateRequest request) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(request.courseId());

        validateTeacherOrAdminAccess(actor, course);

        final var entity = new AssessmentEntity();
        entity.setCourseId(request.courseId());
        entity.setCreatedBy(actor.getId());

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

        final var entity = new AssessmentEntity();
        entity.setCourseId(request.courseId());
        entity.setCreatedBy(actor.getId());
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setQuestionsJson(writeJson(request.questions()));
        entity.setAnswerKeyJson(writeJson(request.answerKey()));
        entity.setRubricJson(writeJson(request.rubricCriteria()));

        if (request.lectureId() != null) {
            final var lecture = lectureRepository.findById(request.lectureId())
                    .orElseThrow(() -> new NotFoundException("Lecture not found: " + request.lectureId()));
            final var lesson = lessonRepository.findById(lecture.getLessonId())
                    .orElseThrow(() -> new NotFoundException("Lesson not found for lecture: " + request.lectureId()));
            if (!lesson.getCourseId().equals(request.courseId())) {
                throw new BadRequestException("Lecture does not belong to the requested course");
            }
            entity.setSourceType("LECTURE");
            entity.setSourceId(request.lectureId());
        } else if (request.lessonId() != null) {
            final var lesson = lessonRepository.findById(request.lessonId())
                    .orElseThrow(() -> new NotFoundException("Lesson not found: " + request.lessonId()));
            if (!lesson.getCourseId().equals(request.courseId())) {
                throw new BadRequestException("Lesson does not belong to the requested course");
            }
            entity.setSourceType("LESSON");
            entity.setSourceId(request.lessonId());
        } else {
            entity.setSourceType("COURSE");
            entity.setSourceId(request.courseId());
        }

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

    private void validateTeacherOrAdminAccess(UserEntity actor, CourseEntity course) {
        final var canCreate = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canCreate) {
            throw new ForbiddenException("Only course teacher or admin can manage assessments");
        }
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
