package by.gsu.learningplatform.capabilities.admin;

import lombok.val;
import by.gsu.learningplatform.capabilities.courses.CourseRepository;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentRepository;
import by.gsu.learningplatform.capabilities.submissions.SubmissionRepository;
import by.gsu.learningplatform.capabilities.users.UserRepository;
import by.gsu.learningplatform.core.observability.BusinessMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final BusinessMetrics businessMetrics;

    public AdminStatisticsService(UserRepository userRepository,
                                  CourseRepository courseRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  SubmissionRepository submissionRepository,
                                  BusinessMetrics businessMetrics) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.submissionRepository = submissionRepository;
        this.businessMetrics = businessMetrics;
    }

    public PlatformStatisticsResponse getPlatformStatistics() {
        val usersCount = userRepository.count();
        val coursesCount = courseRepository.count();
        val enrollmentsCount = enrollmentRepository.count();
        val avgScore = submissionRepository.averageScore();

        businessMetrics.setActiveUsers(Math.toIntExact(usersCount));
        return new PlatformStatisticsResponse(usersCount, coursesCount, enrollmentsCount, avgScore);
    }
}
