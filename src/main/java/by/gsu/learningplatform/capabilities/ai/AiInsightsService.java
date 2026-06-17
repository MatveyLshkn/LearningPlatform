package by.gsu.learningplatform.capabilities.ai;

import lombok.val;
import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.capabilities.courses.CourseService;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.lectures.LectureRepository;
import by.gsu.learningplatform.capabilities.lessons.LessonRepository;
import by.gsu.learningplatform.capabilities.submissions.SubmissionEntity;
import by.gsu.learningplatform.capabilities.submissions.SubmissionRepository;
import by.gsu.learningplatform.capabilities.users.UserEntity;
import by.gsu.learningplatform.capabilities.users.UserRole;
import by.gsu.learningplatform.capabilities.users.UserService;
import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import by.gsu.learningplatform.core.error.ForbiddenException;
import by.gsu.learningplatform.core.security.AuthFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AiInsightsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiInsightsService.class);
    private static final Pattern CYRILLIC_PATTERN = Pattern.compile("\\p{IsCyrillic}");

    private final CourseService courseService;
    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final LectureRepository lectureRepository;
    private final AuthFacade authFacade;
    private final AiPromptBuilderService aiPromptBuilderService;
    private final AiClientService aiClientService;
    private final AiSafetyService aiSafetyService;
    private final LearningPlatformProperties learningPlatformProperties;

    public AiInsightsService(CourseService courseService,
                             UserService userService,
                             EnrollmentRepository enrollmentRepository,
                             SubmissionRepository submissionRepository,
                             LessonRepository lessonRepository,
                             LectureRepository lectureRepository,
                             AuthFacade authFacade,
                             AiPromptBuilderService aiPromptBuilderService,
                             AiClientService aiClientService,
                             AiSafetyService aiSafetyService,
                             LearningPlatformProperties learningPlatformProperties) {
        this.courseService = courseService;
        this.userService = userService;
        this.enrollmentRepository = enrollmentRepository;
        this.submissionRepository = submissionRepository;
        this.lessonRepository = lessonRepository;
        this.lectureRepository = lectureRepository;
        this.authFacade = authFacade;
        this.aiPromptBuilderService = aiPromptBuilderService;
        this.aiClientService = aiClientService;
        this.aiSafetyService = aiSafetyService;
        this.learningPlatformProperties = learningPlatformProperties;
    }

    @Transactional(readOnly = true)
    public CourseStudentAnalyticsResponse courseAnalytics(final UUID courseId) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = courseService.getEntity(courseId);
        validateTeacherOrAdminAccess(actor, course);
        val language = detectLanguage(course.getTitle(), course.getDescription());

        val submissions = submissionRepository.findByCourseId(courseId);
        val grouped = groupByStudent(submissions);
        val enrolledStudentIds = enrollmentRepository.findByCourseId(courseId).stream()
                .map(enrollment -> enrollment.getUserId())
                .distinct()
                .filter(userId -> userService.getById(userId).getRole() == UserRole.STUDENT)
                .toList();

        val studentSummaries = enrolledStudentIds.stream()
                .map(studentId -> toStudentSummary(studentId, grouped.getOrDefault(studentId, List.of()), language))
                .sorted(Comparator.comparing(StudentSummary::studentId))
                .toList();

        val noDataSummary = localizeNoDataSummary(language);
        val aiUnavailableSummary = localizeAiUnavailableSummary(language);
        var aiSummary = noDataSummary;
        var aiInsights = List.<AiStudentInsightPayload>of();
        if (!studentSummaries.isEmpty()) {
            aiSummary = aiUnavailableSummary;
            try {
                val aiResponse = aiClientService.generate(
                        aiPromptBuilderService.analyticsSystemPrompt(),
                        aiPromptBuilderService.buildAnalyticsUserPrompt(buildAnalyticsInput(course, studentSummaries)));
                val payload = aiSafetyService.parseAnalyticsPayload(aiResponse);
                aiSummary = payload.courseSummary();
                aiInsights = payload.students();
            } catch (AiIntegrationException ex) {
                LOGGER.warn("AI analytics fallback for course {}: {}", courseId, ex.getMessage());
            }
        }

        val insightMap = new HashMap<UUID, AiStudentInsightPayload>();
        for (val insight : aiInsights) {
            insightMap.put(insight.studentId(), insight);
        }

        val students = new ArrayList<StudentAnalyticsResponse>();
        for (val summary : studentSummaries) {
            val insight = insightMap.get(summary.studentId());
            val focus = insight != null ? insight.improvementFocus() : summary.defaultImprovementFocus();
            val confidence = insight != null ? insight.confidence() : summary.defaultConfidence();
            val actions = insight != null && !insight.actions().isEmpty()
                    ? insight.actions()
                    : defaultActions(language);

            students.add(new StudentAnalyticsResponse(
                    summary.studentId(),
                    summary.totalSubmissions(),
                    summary.gradedSubmissions(),
                    summary.averageScore(),
                    summary.trend(),
                    focus,
                    confidence,
                    actions
            ));
        }

        val averageScore = submissionRepository.averageScoreByCourse(courseId);
        val gradedCount = submissionRepository.gradedCountByCourse(courseId);
        val totalCount = submissionRepository.totalCountByCourse(courseId);

        return new CourseStudentAnalyticsResponse(
                courseId,
                averageScore,
                totalCount,
                gradedCount,
                calculateCourseRecencyDelta(submissions),
                aiSummary,
                students
        );
    }

    @Transactional(readOnly = true)
    public AiStudyPlanResponse studyPlan(final UUID courseId, final UUID studentId) {
        val actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        val course = courseService.getEntity(courseId);
        validateTeacherOrAdminAccess(actor, course);
        val student = userService.getById(studentId);
        if (student.getRole() != UserRole.STUDENT || !enrollmentRepository.existsByUserIdAndCourseId(studentId, courseId)) {
            throw new ForbiddenException("Study plan can be generated only for enrolled students");
        }

        val studentSubmissions = submissionRepository.findByCourseIdAndStudentId(courseId, studentId);
        val materialSummary = buildMaterialSummary(courseId);
        val language = detectLanguage(course.getTitle(), course.getDescription(), materialSummary);
        val studentSummary = toStudentSummary(studentId, studentSubmissions, language);

        try {
            val aiResponse = aiClientService.generate(
                    aiPromptBuilderService.studyPlanSystemPrompt(),
                    aiPromptBuilderService.buildStudyPlanUserPrompt(
                            buildStudentPlanInput(course, studentSummary),
                            materialSummary)
            );
            val payload = aiSafetyService.parseStudyPlan(aiResponse);
            return new AiStudyPlanResponse(
                    courseId,
                    studentId,
                    payload.prioritizedGoals(),
                    payload.weeklyTargets(),
                    payload.lessonOrder(),
                    payload.lectureOrder(),
                    payload.rationale()
            );
        } catch (AiIntegrationException ignored) {
            return new AiStudyPlanResponse(
                    courseId,
                    studentId,
                    fallbackPrioritizedGoals(language),
                    fallbackWeeklyTargets(language),
                    lessonRepository.findByCourseId(courseId).stream().map(lesson -> lesson.getTitle()).toList(),
                    List.of(),
                    fallbackStudyPlanRationale(language)
            );
        }
    }

    private Map<UUID, List<SubmissionEntity>> groupByStudent(final List<SubmissionEntity> submissions) {
        val grouped = new HashMap<UUID, List<SubmissionEntity>>();
        for (val submission : submissions) {
            grouped.computeIfAbsent(submission.getStudentId(), key -> new ArrayList<>()).add(submission);
        }
        return grouped;
    }

    private StudentSummary toStudentSummary(final UUID studentId, final List<SubmissionEntity> submissions, final InsightLanguage language) {
        val graded = submissions.stream().filter(s -> s.getScore() != null).toList();
        val average = graded.stream().mapToInt(SubmissionEntity::getScore).average().orElse(0.0d);
        val trend = calculateTrend(graded, language);
        return new StudentSummary(
                studentId,
                submissions.size(),
                graded.size(),
                average,
                trend,
                defaultFocusByAverage(average, language),
                defaultConfidenceByVolume(graded.size())
        );
    }

    private String calculateTrend(final List<SubmissionEntity> gradedSubmissions, final InsightLanguage language) {
        if (gradedSubmissions.size() < 2) {
            return localizeTrendStable(language);
        }
        val sorted = gradedSubmissions.stream()
                .sorted(Comparator.comparing(SubmissionEntity::getSubmittedAt))
                .toList();
        val split = Math.max(1, sorted.size() / 2);
        val olderAverage = sorted.subList(0, split).stream().mapToInt(SubmissionEntity::getScore).average().orElse(0);
        val recentAverage = sorted.subList(split, sorted.size()).stream().mapToInt(SubmissionEntity::getScore).average().orElse(0);
        val delta = recentAverage - olderAverage;
        if (delta > 3) {
            return localizeTrendImproving(language);
        }
        if (delta < -3) {
            return localizeTrendDeclining(language);
        }
        return localizeTrendStable(language);
    }

    private Double calculateCourseRecencyDelta(final List<SubmissionEntity> submissions) {
        val recentFrom = OffsetDateTime.now().minusDays(learningPlatformProperties.ai().analyticsRecentDays());
        val recent = submissions.stream()
                .filter(s -> s.getScore() != null && !s.getSubmittedAt().isBefore(recentFrom))
                .mapToInt(SubmissionEntity::getScore)
                .average()
                .orElse(0);
        val older = submissions.stream()
                .filter(s -> s.getScore() != null && s.getSubmittedAt().isBefore(recentFrom))
                .mapToInt(SubmissionEntity::getScore)
                .average()
                .orElse(0);
        return recent - older;
    }

    private String buildAnalyticsInput(final CourseEntity course, final List<StudentSummary> summaries) {
        val language = detectLanguage(course.getTitle(), course.getDescription());
        val builder = new StringBuilder();
        if (language == InsightLanguage.RUSSIAN) {
            builder.append("Название курса: ").append(course.getTitle()).append('\n');
            if (course.getDescription() != null && !course.getDescription().isBlank()) {
                builder.append("Описание курса: ").append(course.getDescription()).append('\n');
            }
            for (val summary : summaries) {
                builder.append("Студент: ").append(summary.studentId())
                        .append(", всего отправок=").append(summary.totalSubmissions())
                        .append(", проверенных отправок=").append(summary.gradedSubmissions())
                        .append(", средний балл=").append(summary.averageScore())
                        .append(", тренд=").append(summary.trend())
                        .append(", фокус улучшения=").append(summary.defaultImprovementFocus())
                        .append('\n');
            }
            return builder.toString();
        }
        builder.append("Course title: ").append(course.getTitle()).append('\n');
        if (course.getDescription() != null && !course.getDescription().isBlank()) {
            builder.append("Course description: ").append(course.getDescription()).append('\n');
        }
        for (val summary : summaries) {
            builder.append("Student: ").append(summary.studentId())
                    .append(", totalSubmissions=").append(summary.totalSubmissions())
                    .append(", gradedSubmissions=").append(summary.gradedSubmissions())
                    .append(", avg=").append(summary.averageScore())
                    .append(", trend=").append(summary.trend())
                    .append(", defaultImprovementFocus=").append(summary.defaultImprovementFocus())
                    .append('\n');
        }
        return builder.toString();
    }

    private String buildMaterialSummary(final UUID courseId) {
        val lessons = lessonRepository.findByCourseId(courseId);
        val language = detectLanguage(lessons.stream().map(lesson -> lesson.getTitle() + " " + lesson.getContent()).toArray(String[]::new));
        val builder = new StringBuilder();
        for (val lesson : lessons) {
            builder.append(language == InsightLanguage.RUSSIAN ? "Урок: " : "Lesson: ").append(lesson.getTitle()).append('\n');
            val lectures = lectureRepository.findByLessonId(lesson.getId());
            for (val lecture : lectures) {
                builder.append(language == InsightLanguage.RUSSIAN ? "Лекция: " : "Lecture: ").append(lecture.getTitle()).append('\n');
            }
        }
        return builder.toString();
    }

    private String buildStudentPlanInput(final CourseEntity course, final StudentSummary summary) {
        val language = detectLanguage(course.getTitle(), course.getDescription(), summary.defaultImprovementFocus(), summary.trend());
        if (language == InsightLanguage.RUSSIAN) {
            return """
                    Курс: %s
                    StudentId: %s
                    Всего отправок: %d
                    Проверенных отправок: %d
                    Средний балл: %.2f
                    Тренд: %s
                    Фокус улучшения: %s
                    """.formatted(
                    course.getTitle(),
                    summary.studentId(),
                    summary.totalSubmissions(),
                    summary.gradedSubmissions(),
                    summary.averageScore(),
                    summary.trend(),
                    summary.defaultImprovementFocus()
            );
        }
        return """
                Course: %s
                StudentId: %s
                totalSubmissions: %d
                gradedSubmissions: %d
                averageScore: %.2f
                trend: %s
                defaultImprovementFocus: %s
                """.formatted(
                course.getTitle(),
                summary.studentId(),
                summary.totalSubmissions(),
                summary.gradedSubmissions(),
                summary.averageScore(),
                summary.trend(),
                summary.defaultImprovementFocus()
        );
    }

    private String defaultFocusByAverage(final double average, final InsightLanguage language) {
        if (average < 60) {
            return language == InsightLanguage.RUSSIAN
                    ? "Базовые знания и ключевые концепции"
                    : "Fundamentals and core concepts";
        }
        if (average < 80) {
            return language == InsightLanguage.RUSSIAN
                    ? "Стабильность и более глубокое понимание"
                    : "Consistency and deeper understanding";
        }
        return language == InsightLanguage.RUSSIAN
                ? "Продвинутое применение и скорость"
                : "Advanced application and speed";
    }

    private Double defaultConfidenceByVolume(final int gradedSubmissions) {
        if (gradedSubmissions == 0) {
            return 0.2;
        }
        if (gradedSubmissions < 3) {
            return 0.5;
        }
        if (gradedSubmissions < 6) {
            return 0.7;
        }
        return 0.85;
    }

    private void validateTeacherOrAdminAccess(final UserEntity actor, final CourseEntity course) {
        val canView = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canView) {
            throw new ForbiddenException("Only course teacher or admin can use course AI insights");
        }
    }

    private InsightLanguage detectLanguage(final String... values) {
        for (val value : values) {
            if (value != null && CYRILLIC_PATTERN.matcher(value).find()) {
                return InsightLanguage.RUSSIAN;
            }
        }
        return InsightLanguage.DEFAULT;
    }

    private String localizeNoDataSummary(final InsightLanguage language) {
        return language == InsightLanguage.RUSSIAN
                ? "Недостаточно данных для генерации AI-сводки"
                : "Insufficient data to generate AI summary";
    }

    private String localizeTrendStable(final InsightLanguage language) {
        return language == InsightLanguage.RUSSIAN ? "стабильный" : "stable";
    }

    private String localizeTrendImproving(final InsightLanguage language) {
        return language == InsightLanguage.RUSSIAN ? "улучшается" : "improving";
    }

    private String localizeTrendDeclining(final InsightLanguage language) {
        return language == InsightLanguage.RUSSIAN ? "снижается" : "declining";
    }

    private String localizeAiUnavailableSummary(final InsightLanguage language) {
        return language == InsightLanguage.RUSSIAN
                ? "AI-провайдер недоступен. Показана детерминированная аналитика."
                : "AI provider unavailable. Showing deterministic analytics.";
    }

    private List<String> defaultActions(final InsightLanguage language) {
        if (language == InsightLanguage.RUSSIAN) {
            return List.of(
                    "Повторить ключевые концепции из последних уроков",
                    "Ежедневно решать по одному дополнительному упражнению"
            );
        }
        return List.of(
                "Revise key concepts from recent lessons",
                "Practice one extra exercise daily"
        );
    }

    private List<String> fallbackPrioritizedGoals(final InsightLanguage language) {
        if (language == InsightLanguage.RUSSIAN) {
            return List.of("Повысить средний балл минимум на 10 пунктов в следующих двух оцениваниях");
        }
        return List.of("Raise average score by at least 10 points in next two assessments");
    }

    private List<String> fallbackWeeklyTargets(final InsightLanguage language) {
        if (language == InsightLanguage.RUSSIAN) {
            return List.of(
                    "Проводить 3 целевые практические сессии в неделю",
                    "Раз в неделю отправлять 1 самопроверку с краткой рефлексией"
            );
        }
        return List.of(
                "Complete 3 focused practice sessions per week",
                "Submit 1 self-check reflection per week"
        );
    }

    private String fallbackStudyPlanRationale(final InsightLanguage language) {
        return language == InsightLanguage.RUSSIAN
                ? "Резервный план сформирован из-за некорректного или недоступного AI-ответа."
                : "Fallback plan generated because AI output was invalid.";
    }

    private enum InsightLanguage {
        DEFAULT,
        RUSSIAN
    }

    private record StudentSummary(UUID studentId,
                                  int totalSubmissions,
                                  int gradedSubmissions,
                                  double averageScore,
                                  String trend,
                                  String defaultImprovementFocus,
                                  double defaultConfidence) {
    }
}
