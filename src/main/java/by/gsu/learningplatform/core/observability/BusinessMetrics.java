package by.gsu.learningplatform.core.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BusinessMetrics {

    private final Counter registrationsCounter;
    private final Counter coursesCounter;
    private final Counter enrollmentsCounter;
    private final Counter submissionsCounter;
    private final Counter gradedSubmissionsCounter;
    private final AtomicInteger activeUsersGauge = new AtomicInteger(0);

    public BusinessMetrics(final MeterRegistry meterRegistry) {
        this.registrationsCounter = Counter.builder("learningplatform_registrations_total")
                .description("Total registered users")
                .register(meterRegistry);
        this.coursesCounter = Counter.builder("learningplatform_courses_total")
                .description("Total created courses")
                .register(meterRegistry);
        this.enrollmentsCounter = Counter.builder("learningplatform_enrollments_total")
                .description("Total enrollments")
                .register(meterRegistry);
        this.submissionsCounter = Counter.builder("learningplatform_submissions_total")
                .description("Total created submissions")
                .register(meterRegistry);
        this.gradedSubmissionsCounter = Counter.builder("learningplatform_submissions_graded_total")
                .description("Total graded submissions")
                .register(meterRegistry);
        Gauge.builder("learningplatform_active_users", activeUsersGauge, AtomicInteger::get)
                .description("Approximate active users")
                .register(meterRegistry);
    }

    public void incrementEnrollments() {
        enrollmentsCounter.increment();
    }

    public void incrementRegistrations() {
        registrationsCounter.increment();
    }

    public void incrementCourses() {
        coursesCounter.increment();
    }

    public void incrementSubmissions() {
        submissionsCounter.increment();
    }

    public void incrementGradedSubmissions() {
        gradedSubmissionsCounter.increment();
    }

    public void setActiveUsers(final int value) {
        activeUsersGauge.set(value);
    }
}
