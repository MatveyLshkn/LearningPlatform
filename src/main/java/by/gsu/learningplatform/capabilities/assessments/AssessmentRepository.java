package by.gsu.learningplatform.capabilities.assessments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    List<AssessmentEntity> findByCourseId(UUID courseId);

    long countByCreatedBy(UUID teacherId);
}
