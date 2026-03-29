package by.gsu.learningplatform.capabilities.admin;

public record PlatformStatisticsResponse(long usersCount,
                                         long coursesCount,
                                         long enrollmentsCount,
                                         Double averageSubmissionScore) {
}
