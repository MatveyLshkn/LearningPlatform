package by.gsu.learningplatform.capabilities.ai;

import lombok.val;
import by.gsu.learningplatform.capabilities.courses.CourseEntity;
import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiInsightsServiceUnitTest {

    @Test
    void shouldDetectRussianForCyrillicSource() throws Exception {
        val service = new AiInsightsService(
                null, null, null, null, null, null, null, null, null, null,
                new LearningPlatformProperties(
                        new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                        new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                        new LearningPlatformProperties.PaginationProperties(20, 100),
                        new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                        new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 200, 21, 20)
                )
        );

        val language = invoke(service, "detectLanguage", new Class<?>[]{String[].class}, new Object[]{new String[]{"Курс по алгебре"}});
        val summary = invoke(service, "localizeNoDataSummary", new Class<?>[]{language.getClass()}, language);
        val actions = invoke(service, "defaultActions", new Class<?>[]{language.getClass()}, language);
        val rationale = invoke(service, "fallbackStudyPlanRationale", new Class<?>[]{language.getClass()}, language);

        assertEquals("RUSSIAN", language.toString());
        assertEquals("Недостаточно данных для генерации AI-сводки", summary);
        assertEquals(
                List.of(
                        "Повторить ключевые концепции из последних уроков",
                        "Ежедневно решать по одному дополнительному упражнению"
                ),
                actions
        );
        assertTrue(rationale.toString().contains("Резервный план"));
    }

    @Test
    void shouldBuildRussianAnalyticsAndStudyPlanInputs() throws Exception {
        val service = new AiInsightsService(
                null, null, null, null, null, null, null, null, null, null,
                new LearningPlatformProperties(
                        new LearningPlatformProperties.SecurityProperties("preferred_username", "realm_access.roles"),
                        new LearningPlatformProperties.KeycloakProperties("http://localhost:8081", "learning-platform", "client", "secret", "admin", "admin"),
                        new LearningPlatformProperties.PaginationProperties(20, 100),
                        new LearningPlatformProperties.LiquibaseProperties("classpath:/db/changelog/db.changelog-master.yaml", true),
                        new LearningPlatformProperties.AiProperties("qwen2.5:7b", 0.2, 1200, 200, 21, 20)
                )
        );
        val course = new CourseEntity();
        course.setTitle("Алгебра");
        course.setDescription("Квадратные уравнения");
        val studentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        val summary = createStudentSummary(
                service,
                studentId,
                4,
                3,
                78.5,
                "стабильный",
                "Стабильность и более глубокое понимание",
                0.7
        );

        val analyticsInput = invoke(service, "buildAnalyticsInput", new Class<?>[]{CourseEntity.class, List.class}, course, List.of(summary));
        val studyPlanInput = invoke(service, "buildStudentPlanInput", new Class<?>[]{CourseEntity.class, summary.getClass()}, course, summary);

        assertTrue(analyticsInput.toString().contains("Название курса: Алгебра"));
        assertTrue(analyticsInput.toString().contains("Студент: " + studentId));
        assertTrue(analyticsInput.toString().contains("тренд=стабильный"));
        assertTrue(studyPlanInput.toString().contains("Курс: Алгебра"));
        assertTrue(studyPlanInput.toString().contains("Фокус улучшения: Стабильность и более глубокое понимание"));
    }

    private Object createStudentSummary(
            final AiInsightsService service,
            final UUID studentId,
            final int totalSubmissions,
            final int gradedSubmissions,
            final double averageScore,
            final String trend,
            final String defaultImprovementFocus,
            final double defaultConfidence
    ) throws Exception {
        final Class<?> studentSummaryClass = Class.forName("by.gsu.learningplatform.capabilities.ai.AiInsightsService$StudentSummary");
        final var constructor = studentSummaryClass.getDeclaredConstructor(
                UUID.class,
                int.class,
                int.class,
                double.class,
                String.class,
                String.class,
                double.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(
                studentId,
                totalSubmissions,
                gradedSubmissions,
                averageScore,
                trend,
                defaultImprovementFocus,
                defaultConfidence
        );
    }

    private Object invoke(
            final AiInsightsService service,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = AiInsightsService.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }
}
