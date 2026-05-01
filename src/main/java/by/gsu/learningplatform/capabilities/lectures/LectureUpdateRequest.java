package by.gsu.learningplatform.capabilities.lectures;

import jakarta.validation.constraints.Size;

public record LectureUpdateRequest(@Size(max = 255) String title,
                                   String videoUrl,
                                   String content) {
}
