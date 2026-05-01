package by.gsu.learningplatform.capabilities.ai;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiInsightsService {

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
    public CourseStudentAnalyticsResponse courseAnalytics(UUID courseId) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(courseId);
        validateTeacherOrAdminAccess(actor, course);

        final var submissions = submissionRepository.findByCourseId(courseId);
        final var grouped = groupByStudent(submissions);
        final var enrolledStudentIds = enrollmentRepository.findByCourseId(courseId).stream()
                .map(enrollment -> enrollment.getUserId())
                .distinct()
                .filter(userId -> userService.getById(userId).getRole() == UserRole.STUDENT)
                .toList();

        final var studentSummaries = enrolledStudentIds.stream()
                .map(studentId -> toStudentSummary(studentId, grouped.getOrDefault(studentId, List.of())))
                .sorted(Comparator.comparing(StudentSummary::studentId))
                .toList();

        final var defaultSummary = "Insufficient data to generate AI summary";
        var aiSummary = defaultSummary;
        var aiInsights = List.<AiStudentInsightPayload>of();
        if (!studentSummaries.isEmpty()) {
            try {
                final var aiResponse = aiClientService.generate(
                        aiPromptBuilderService.analyticsSystemPrompt(),
                        aiPromptBuilderService.buildAnalyticsUserPrompt(buildAnalyticsInput(course, studentSummaries)));
                final var payload = aiSafetyService.parseAnalyticsPayload(aiResponse);
                aiSummary = payload.courseSummary();
                aiInsights = payload.students();
            } catch (AiIntegrationException ignored) {
                aiSummary = defaultSummary;
            }
        }

        final var insightMap = new HashMap<UUID, AiStudentInsightPayload>();
        for (var insight : aiInsights) {
            insightMap.put(insight.studentId(), insight);
        }

        final var students = new ArrayList<StudentAnalyticsResponse>();
        for (var summary : studentSummaries) {
            final var insight = insightMap.get(summary.studentId());
            final var focus = insight != null ? insight.improvementFocus() : summary.defaultImprovementFocus();
            final var confidence = insight != null ? insight.confidence() : summary.defaultConfidence();
            final var actions = insight != null && !insight.actions().isEmpty()
                    ? insight.actions()
                    : List.of("Revise key concepts from recent lessons", "Practice one extra exercise daily");

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

        final var averageScore = submissionRepository.averageScoreByCourse(courseId);
        final var gradedCount = submissionRepository.gradedCountByCourse(courseId);
        final var totalCount = submissionRepository.totalCountByCourse(courseId);

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
    public AiStudyPlanResponse studyPlan(UUID courseId, UUID studentId) {
        final var actor = userService.getByKeycloakSub(authFacade.currentPrincipal().keycloakSub());
        final var course = courseService.getEntity(courseId);
        validateTeacherOrAdminAccess(actor, course);
        final var student = userService.getById(studentId);
        if (student.getRole() != UserRole.STUDENT || !enrollmentRepository.existsByUserIdAndCourseId(studentId, courseId)) {
            throw new ForbiddenException("Study plan can be generated only for enrolled students");
        }

        final var studentSubmissions = submissionRepository.findByCourseIdAndStudentId(courseId, studentId);
        final var studentSummary = toStudentSummary(studentId, studentSubmissions);
        final var materialSummary = buildMaterialSummary(courseId);

        try {
            final var aiResponse = aiClientService.generate(
                    aiPromptBuilderService.studyPlanSystemPrompt(),
                    aiPromptBuilderService.buildStudyPlanUserPrompt(
                            buildStudentPlanInput(course, studentSummary),
                            materialSummary)
            );
            final var payload = aiSafetyService.parseStudyPlan(aiResponse);
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
                    List.of("Raise average score by at least 10 points in next two assessments"),
                    List.of("Complete 3 focused practice sessions per week", "Submit 1 self-check reflection per week"),
                    lessonRepository.findByCourseId(courseId).stream().map(lesson -> lesson.getTitle()).toList(),
                    List.of(),
                    "Fallback plan generated because AI output was invalid."
            );
        }
    }

    private Map<UUID, List<SubmissionEntity>> groupByStudent(List<SubmissionEntity> submissions) {
        final var grouped = new HashMap<UUID, List<SubmissionEntity>>();
        for (var submission : submissions) {
            grouped.computeIfAbsent(submission.getStudentId(), key -> new ArrayList<>()).add(submission);
        }
        return grouped;
    }

    private StudentSummary toStudentSummary(UUID studentId, List<SubmissionEntity> submissions) {
        final var graded = submissions.stream().filter(s -> s.getScore() != null).toList();
        final var average = graded.stream().mapToInt(SubmissionEntity::getScore).average().orElse(0.0d);
        final var trend = calculateTrend(graded);
        return new StudentSummary(
                studentId,
                submissions.size(),
                graded.size(),
                average,
                trend,
                defaultFocusByAverage(average),
                defaultConfidenceByVolume(graded.size())
        );
    }

    private String calculateTrend(List<SubmissionEntity> gradedSubmissions) {
        if (gradedSubmissions.size() < 2) {
            return "stable";
        }
        final var sorted = gradedSubmissions.stream()
                .sorted(Comparator.comparing(SubmissionEntity::getSubmittedAt))
                .toList();
        final var split = Math.max(1, sorted.size() / 2);
        final var olderAverage = sorted.subList(0, split).stream().mapToInt(SubmissionEntity::getScore).average().orElse(0);
        final var recentAverage = sorted.subList(split, sorted.size()).stream().mapToInt(SubmissionEntity::getScore).average().orElse(0);
        final var delta = recentAverage - olderAverage;
        if (delta > 3) {
            return "improving";
        }
        if (delta < -3) {
            return "declining";
        }
        return "stable";
    }

    private Double calculateCourseRecencyDelta(List<SubmissionEntity> submissions) {
        final var recentFrom = OffsetDateTime.now().minusDays(learningPlatformProperties.ai().analyticsRecentDays());
        final var recent = submissions.stream()
                .filter(s -> s.getScore() != null && !s.getSubmittedAt().isBefore(recentFrom))
                .mapToInt(SubmissionEntity::getScore)
                .average()
                .orElse(0);
        final var older = submissions.stream()
                .filter(s -> s.getScore() != null && s.getSubmittedAt().isBefore(recentFrom))
                .mapToInt(SubmissionEntity::getScore)
                .average()
                .orElse(0);
        return recent - older;
    }

    private String buildAnalyticsInput(CourseEntity course, List<StudentSummary> summaries) {
        final var builder = new StringBuilder();
        builder.append("Course title: ").append(course.getTitle()).append('\n');
        for (var summary : summaries) {
            builder.append("Student: ").append(summary.studentId())
                    .append(", totalSubmissions=").append(summary.totalSubmissions())
                    .append(", gradedSubmissions=").append(summary.gradedSubmissions())
                    .append(", avg=").append(summary.averageScore())
                    .append(", trend=").append(summary.trend())
                    .append('\n');
        }
        return builder.toString();
    }

    private String buildMaterialSummary(UUID courseId) {
        final var lessons = lessonRepository.findByCourseId(courseId);
        final var builder = new StringBuilder();
        for (var lesson : lessons) {
            builder.append("Lesson: ").append(lesson.getTitle()).append('\n');
            final var lectures = lectureRepository.findByLessonId(lesson.getId());
            for (var lecture : lectures) {
                builder.append("Lecture: ").append(lecture.getTitle()).append('\n');
            }
        }
        return builder.toString();
    }

    private String buildStudentPlanInput(CourseEntity course, StudentSummary summary) {
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

    private String defaultFocusByAverage(double average) {
        if (average < 60) {
            return "Fundamentals and core concepts";
        }
        if (average < 80) {
            return "Consistency and deeper understanding";
        }
        return "Advanced application and speed";
    }

    private Double defaultConfidenceByVolume(int gradedSubmissions) {
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

    private void validateTeacherOrAdminAccess(UserEntity actor, CourseEntity course) {
        final var canView = actor.getRole() == UserRole.ADMIN || actor.getId().equals(course.getTeacherId());
        if (!canView) {
            throw new ForbiddenException("Only course teacher or admin can use course AI insights");
        }
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
