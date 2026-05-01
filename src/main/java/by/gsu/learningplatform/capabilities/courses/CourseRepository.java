package by.gsu.learningplatform.capabilities.courses;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {

    Page<CourseEntity> findByTeacherId(UUID teacherId, Pageable pageable);

    List<CourseEntity> findByTeacherId(UUID teacherId);

    List<CourseEntity> findByIdIn(Collection<UUID> ids);

    @Query("""
            select c
            from CourseEntity c
            join c.tags t
            where t.name in :tagNames
            group by c
            having count(distinct t.name) = :tagCount
            """)
    Page<CourseEntity> findByAllTagNames(Collection<String> tagNames, long tagCount, Pageable pageable);

    @Query("""
            select c
            from CourseEntity c
            join EnrollmentEntity e on e.courseId = c.id
            where e.userId = :userId
            """)
    Page<CourseEntity> findEnrolledByUserId(UUID userId, Pageable pageable);
}
