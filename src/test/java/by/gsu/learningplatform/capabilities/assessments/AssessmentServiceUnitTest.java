package by.gsu.learningplatform.capabilities.assessments;

import lombok.val;
import by.gsu.learningplatform.capabilities.ai.AiAssessmentDraftPayload;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentServiceUnitTest {

    @Test
    void shouldBuildRussianFallbackDraftForCyrillicSource() throws Exception {
        val service = new AssessmentService(null, null, null, null, null, null, null, null, null, null, null, null, null);

        val payload = invokeFallbackDraftPayload(service, "Алгебра", 2, "medium", "Тема урока: квадратные уравнения");

        assertTrue(payload.title().contains("Черновик теста"));
        assertTrue(payload.description().contains("Резервный тест"));
        assertTrue(payload.questions().getFirst().startsWith("Вопрос 1."));
        assertTrue(payload.answerKey().getFirst().startsWith("Ответ 1."));
        assertTrue(payload.rubricCriteria().contains("Ясность и структура ответа"));
    }

    private AiAssessmentDraftPayload invokeFallbackDraftPayload(
            final AssessmentService service,
            final String courseTitle,
            final int questionCount,
            final String difficulty,
            final String source
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = AssessmentService.class.getDeclaredMethod(
                "fallbackDraftPayload",
                String.class,
                int.class,
                String.class,
                String.class
        );
        method.setAccessible(true);
        return (AiAssessmentDraftPayload) method.invoke(service, courseTitle, questionCount, difficulty, source);
    }
}
