package by.gsu.learningplatform.capabilities.progress;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}")
public class CourseProgressController {

    private final CourseProgressService courseProgressService;

    public CourseProgressController(final CourseProgressService courseProgressService) {
        this.courseProgressService = courseProgressService;
    }

    @GetMapping("/progress/me")
    public CourseProgressResponse ownProgress( @PathVariable final UUID courseId) {
        return courseProgressService.getOwnProgress(courseId);
    }

    @GetMapping("/users/{userId}/progress")
    public CourseProgressResponse userProgress( @PathVariable final UUID courseId, @PathVariable final UUID userId) {
        return courseProgressService.getProgress(courseId, userId);
    }

    @PutMapping("/users/{userId}/progress/lectures/{lectureId}")
    public CourseProgressResponse updateLectureProgress(@PathVariable UUID courseId,
                                                        @PathVariable UUID userId,
                                                        @PathVariable UUID lectureId,
                                                        @Valid @RequestBody LectureProgressUpdateRequest request) {
        return courseProgressService.updateLectureProgress(courseId, userId, lectureId, request);
    }
}
