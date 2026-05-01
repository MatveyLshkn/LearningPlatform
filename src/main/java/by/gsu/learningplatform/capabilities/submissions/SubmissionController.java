package by.gsu.learningplatform.capabilities.submissions;

import jakarta.validation.Valid;
import by.gsu.learningplatform.core.web.CursorPageResponse;
import by.gsu.learningplatform.core.web.PaginationUtils;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final PaginationUtils paginationUtils;

    public SubmissionController(SubmissionService submissionService, PaginationUtils paginationUtils) {
        this.submissionService = submissionService;
        this.paginationUtils = paginationUtils;
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
    public CursorPageResponse<SubmissionResponse> listOwn(@org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
                                                          @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor) {
        final var pageable = paginationUtils.toPageable(limit, cursor);
        final var page = submissionService.listOwn(pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/submissions/me?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }

    @GetMapping("/assessment/{assessmentId}")
    public CursorPageResponse<SubmissionResponse> listByAssessment(@PathVariable UUID assessmentId,
                                                                   @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
                                                                   @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor) {
        final var pageable = paginationUtils.toPageable(limit, cursor);
        final var page = submissionService.listByAssessment(assessmentId, pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/submissions/assessment/" + assessmentId + "?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }
}
