package by.gsu.learningplatform.capabilities.users;

import by.gsu.learningplatform.capabilities.assessments.AssessmentResponse;
import by.gsu.learningplatform.capabilities.courses.CourseResponse;
import by.gsu.learningplatform.capabilities.enrollments.EnrollmentResponse;
import by.gsu.learningplatform.capabilities.submissions.SubmissionResponse;

import java.util.List;

public record UserDetailsResponse(UserResponse user,
                                  List<EnrollmentResponse> enrollments,
                                  List<CourseResponse> enrolledCourses,
                                  List<CourseResponse> taughtCourses,
                                  List<SubmissionResponse> submissions,
                                  List<AssessmentResponse> createdAssessments) {
}
