package by.gsu.learningplatform.capabilities.submissions;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubmissionResponse(UUID id,
                                 UUID assessmentId,
                                 UUID studentId,
                                 String answerText,
                                 Integer score,
                                 OffsetDateTime submittedAt,
                                 OffsetDateTime gradedAt) {
}
