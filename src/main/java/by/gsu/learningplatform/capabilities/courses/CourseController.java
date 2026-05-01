package by.gsu.learningplatform.capabilities.courses;

import by.gsu.learningplatform.core.web.CursorPageResponse;
import by.gsu.learningplatform.core.web.PaginationUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final PaginationUtils paginationUtils;

    public CourseController(CourseService courseService, PaginationUtils paginationUtils) {
        this.courseService = courseService;
        this.paginationUtils = paginationUtils;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(request);
    }

    @GetMapping
    public CursorPageResponse<CourseResponse> list(@RequestParam(required = false) Integer limit,
                                                   @RequestParam(required = false) String cursor,
                                                   @RequestParam(name = "tag", required = false) List<String> tags) {
        final var pageable = paginationUtils.toPageable(limit, cursor);
        final var page = courseService.findAll(pageable, tags);
        final var tagsParam = tags == null || tags.isEmpty() ? "" : tags.stream().map(tag -> "&tag=" + tag).reduce("", String::concat);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/courses?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber() + tagsParam));
    }

    @GetMapping("/{courseId}")
    public CourseCatalogDetailsResponse getCatalogDetails(@PathVariable UUID courseId) {
        return courseService.getCatalogDetails(courseId);
    }

    @GetMapping("/enrolled/me")
    public CursorPageResponse<CourseEnrollmentResponse> listEnrolled(@RequestParam(required = false) Integer limit,
                                                                     @RequestParam(required = false) String cursor) {
        final var pageable = paginationUtils.toPageable(limit, cursor);
        final var page = courseService.findEnrolledWithEnrollment(pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/courses/enrolled/me?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }

    @GetMapping("/me")
    public CursorPageResponse<CourseResponse> listOwn(@RequestParam(required = false) Integer limit,
                                                      @RequestParam(required = false) String cursor) {
        final var pageable = paginationUtils.toPageable(limit, cursor);
        final var page = courseService.findOwn(pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/courses/me?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }

    @PatchMapping("/{courseId}")
    public CourseResponse update(@PathVariable UUID courseId, @Valid @RequestBody CourseRequest request) {
        return courseService.update(courseId, request);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID courseId) {
        courseService.delete(courseId);
    }
}
