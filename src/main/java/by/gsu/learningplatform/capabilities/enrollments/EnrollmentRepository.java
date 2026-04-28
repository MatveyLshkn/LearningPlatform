package by.gsu.learningplatform.capabilities.enrollments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {

    Optional<EnrollmentEntity> findByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    List<EnrollmentEntity> findByUserId(UUID userId);

    List<EnrollmentEntity> findByUserIdIn(Collection<UUID> userIds);

    long countByCourseId(UUID courseId);
}
