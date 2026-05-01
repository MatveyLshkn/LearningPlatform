package by.gsu.learningplatform.capabilities.enrollments;

import lombok.val;
import jakarta.validation.Valid;
import by.gsu.learningplatform.core.web.CursorPageResponse;
import by.gsu.learningplatform.core.web.PaginationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final PaginationUtils paginationUtils;

    public EnrollmentController(final EnrollmentService enrollmentService, final PaginationUtils paginationUtils) {
        this.enrollmentService = enrollmentService;
        this.paginationUtils = paginationUtils;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll( @Valid @RequestBody final EnrollmentCreateRequest request) {
        return enrollmentService.enroll(request);
    }

    @DeleteMapping("/{enrollmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave( @PathVariable final UUID enrollmentId) {
        enrollmentService.leave(enrollmentId);
    }

    @GetMapping
    public CursorPageResponse<EnrollmentResponse> listOwn(@org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
                                                          @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor) {
        val pageable = paginationUtils.toPageable(limit, cursor);
        val page = enrollmentService.listOwn(pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/enrollments?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }
}
