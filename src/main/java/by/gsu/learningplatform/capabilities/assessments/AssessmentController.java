package by.gsu.learningplatform.capabilities.assessments;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentResponse create(@Valid @RequestBody AssessmentCreateRequest request) {
        return assessmentService.create(request);
    }

    @PostMapping("/assessments/generate")
    public AssessmentDraftResponse generate(@Valid @RequestBody AssessmentGenerateRequest request) {
        return assessmentService.generateDraft(request);
    }

    @PostMapping("/assessments/from-draft")
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentResponse createFromDraft(@Valid @RequestBody AssessmentCreateFromDraftRequest request) {
        return assessmentService.createFromDraft(request);
    }

    @GetMapping("/assessments/{assessmentId}")
    public AssessmentStudentResponse getForTaking(@PathVariable UUID assessmentId) {
        return assessmentService.getStudentView(assessmentId);
    }

    @GetMapping("/assessments/{assessmentId}/details")
    public AssessmentResponse getDetails(@PathVariable UUID assessmentId) {
        return assessmentService.getDetails(assessmentId);
    }

    @GetMapping("/courses/{courseId}/assessments")
    public by.gsu.learningplatform.core.web.CursorPageResponse<AssessmentStudentResponse> listByCourse(@PathVariable UUID courseId,
                                                                                                         @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
                                                                                                         @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor) {
        return assessmentService.listByCourse(courseId, limit, cursor);
    }

    @PatchMapping("/assessments/{assessmentId}")
    public AssessmentResponse update(@PathVariable UUID assessmentId, @Valid @RequestBody AssessmentUpdateRequest request) {
        return assessmentService.update(assessmentId, request);
    }

    @DeleteMapping("/assessments/{assessmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID assessmentId) {
        assessmentService.delete(assessmentId);
    }
}
