package by.gsu.learningplatform.capabilities.assessments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    List<AssessmentEntity> findByCourseId(UUID courseId);
    Page<AssessmentEntity> findByCourseId(UUID courseId, Pageable pageable);

    List<AssessmentEntity> findByCreatedBy(UUID teacherId);

    long countByCreatedBy(UUID teacherId);
}
