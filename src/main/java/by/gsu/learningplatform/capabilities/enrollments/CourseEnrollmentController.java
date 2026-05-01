package by.gsu.learningplatform.capabilities.enrollments;

import lombok.val;
import by.gsu.learningplatform.capabilities.users.UserResponse;
import by.gsu.learningplatform.core.web.CursorPageResponse;
import by.gsu.learningplatform.core.web.PaginationUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
public class CourseEnrollmentController {

    private final EnrollmentService enrollmentService;
    private final PaginationUtils paginationUtils;

    public CourseEnrollmentController(final EnrollmentService enrollmentService, final PaginationUtils paginationUtils) {
        this.enrollmentService = enrollmentService;
        this.paginationUtils = paginationUtils;
    }

    @GetMapping("/{courseId}/students")
    public CursorPageResponse<UserResponse> listCourseStudents(@PathVariable UUID courseId,
                                                               @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
                                                               @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor) {
        val pageable = paginationUtils.toPageable(limit, cursor);
        val page = enrollmentService.listCourseStudents(courseId, pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/courses/" + courseId + "/students?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }
}
