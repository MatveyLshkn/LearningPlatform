package by.gsu.learningplatform.capabilities.ai;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/courses")
public class AiInsightsController {

    private final AiInsightsService aiInsightsService;

    public AiInsightsController(AiInsightsService aiInsightsService) {
        this.aiInsightsService = aiInsightsService;
    }

    @GetMapping("/{courseId}/ai/student-analytics")
    public CourseStudentAnalyticsResponse studentAnalytics(@PathVariable UUID courseId) {
        return aiInsightsService.courseAnalytics(courseId);
    }

    @GetMapping("/{courseId}/students/{studentId}/ai-study-plan")
    public AiStudyPlanResponse studyPlan(@PathVariable UUID courseId, @PathVariable UUID studentId) {
        return aiInsightsService.studyPlan(courseId, studentId);
    }
}
