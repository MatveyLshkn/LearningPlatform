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

    @Query("""
            select avg(s.score)
            from SubmissionEntity s
            join AssessmentEntity a on a.id = s.assessmentId
            where a.courseId = :courseId and s.score is not null
            """)
    Double averageScoreByCourse(UUID courseId);

    @Query("""
            select count(s)
            from SubmissionEntity s
            join AssessmentEntity a on a.id = s.assessmentId
            where a.courseId = :courseId and s.score is not null
            """)
    long gradedCountByCourse(UUID courseId);

    @Query("""
            select count(s)
            from SubmissionEntity s
            join AssessmentEntity a on a.id = s.assessmentId
            where a.courseId = :courseId
            """)
    long totalCountByCourse(UUID courseId);

    @Query("""
            select s
            from SubmissionEntity s
            join AssessmentEntity a on a.id = s.assessmentId
            where a.courseId = :courseId
            """)
    List<SubmissionEntity> findByCourseId(UUID courseId);

    @Query("""
            select s
            from SubmissionEntity s
            join AssessmentEntity a on a.id = s.assessmentId
            where a.courseId = :courseId and s.studentId = :studentId
            """)
    List<SubmissionEntity> findByCourseIdAndStudentId(UUID courseId, UUID studentId);
}
