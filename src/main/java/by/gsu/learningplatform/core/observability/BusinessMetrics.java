package by.gsu.learningplatform.core.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BusinessMetrics {

    private final Counter enrollmentsCounter;
    private final Counter gradedSubmissionsCounter;
    private final AtomicInteger activeUsersGauge = new AtomicInteger(0);

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.enrollmentsCounter = Counter.builder("learningplatform_enrollments_total")
                .description("Total enrollments")
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

    public void incrementGradedSubmissions() {
        gradedSubmissionsCounter.increment();
    }

    public void setActiveUsers(int value) {
        activeUsersGauge.set(value);
    }
}
