package by.gsu.learningplatform.capabilities.submissions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {

    List<SubmissionEntity> findByStudentId(UUID studentId);

    List<SubmissionEntity> findByAssessmentId(UUID assessmentId);

    @Query("select avg(s.score) from SubmissionEntity s where s.score is not null")
    Double averageScore();
}
