package by.gsu.learningplatform.capabilities.submissions;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submit(@Valid @RequestBody SubmissionCreateRequest request) {
        return submissionService.submit(request);
    }

    @PatchMapping("/{submissionId}")
    public SubmissionResponse grade(@PathVariable UUID submissionId,
                                    @Valid @RequestBody SubmissionGradeRequest request) {
        return submissionService.grade(submissionId, request);
    }

    @GetMapping("/me")
    public List<SubmissionResponse> listOwn() {
        return submissionService.listOwn();
    }

    @GetMapping("/assessment/{assessmentId}")
    public List<SubmissionResponse> listByAssessment(@PathVariable UUID assessmentId) {
        return submissionService.listByAssessment(assessmentId);
    }
}
