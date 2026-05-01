package by.gsu.learningplatform.capabilities.lessons;

import jakarta.validation.Valid;
import by.gsu.learningplatform.core.web.CursorPageResponse;
import by.gsu.learningplatform.core.web.PaginationUtils;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class LessonController {

    private final LessonService lessonService;
    private final PaginationUtils paginationUtils;

    public LessonController(LessonService lessonService, PaginationUtils paginationUtils) {
        this.lessonService = lessonService;
        this.paginationUtils = paginationUtils;
    }

    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(@Valid @RequestBody LessonRequest request) {
        return lessonService.create(request);
    }

    @GetMapping("/lessons/{lessonId}")
    public LessonResponse get(@PathVariable UUID lessonId) {
        return lessonService.getById(lessonId);
    }

    @GetMapping("/courses/{courseId}/lessons")
    public CursorPageResponse<LessonResponse> listByCourse(@PathVariable UUID courseId,
                                                           @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
                                                           @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor) {
        final var pageable = paginationUtils.toPageable(limit, cursor);
        final var page = lessonService.listByCourse(courseId, pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/courses/" + courseId + "/lessons?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }

    @PatchMapping("/lessons/{lessonId}")
    public LessonResponse update(@PathVariable UUID lessonId, @Valid @RequestBody LessonUpdateRequest request) {
        return lessonService.update(lessonId, request);
    }

    @DeleteMapping("/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID lessonId) {
        lessonService.delete(lessonId);
    }
}
