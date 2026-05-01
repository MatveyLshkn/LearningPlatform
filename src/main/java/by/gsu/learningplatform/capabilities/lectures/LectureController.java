package by.gsu.learningplatform.capabilities.lectures;

import lombok.val;
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
public class LectureController {

    private final LectureService lectureService;
    private final PaginationUtils paginationUtils;

    public LectureController(final LectureService lectureService, final PaginationUtils paginationUtils) {
        this.lectureService = lectureService;
        this.paginationUtils = paginationUtils;
    }

    @PostMapping("/lectures")
    @ResponseStatus(HttpStatus.CREATED)
    public LectureResponse create( @Valid @RequestBody final LectureRequest request) {
        return lectureService.create(request);
    }

    @GetMapping("/lectures/{lectureId}")
    public LectureResponse get( @PathVariable final UUID lectureId) {
        return lectureService.getById(lectureId);
    }

    @GetMapping("/lessons/{lessonId}/lectures")
    public CursorPageResponse<LectureResponse> listByLesson(@PathVariable UUID lessonId,
                                                            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer limit,
                                                            @org.springframework.web.bind.annotation.RequestParam(required = false) String cursor) {
        val pageable = paginationUtils.toPageable(limit, cursor);
        val page = lectureService.listByLesson(lessonId, pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/lessons/" + lessonId + "/lectures?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber()));
    }

    @PatchMapping("/lectures/{lectureId}")
    public LectureResponse update( @PathVariable final UUID lectureId, @Valid @RequestBody final LectureUpdateRequest request) {
        return lectureService.update(lectureId, request);
    }

    @DeleteMapping("/lectures/{lectureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete( @PathVariable final UUID lectureId) {
        lectureService.delete(lectureId);
    }
}
