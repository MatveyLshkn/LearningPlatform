package by.gsu.learningplatform.capabilities.courses;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {

    Page<CourseEntity> findByTeacherId(UUID teacherId, Pageable pageable);
}
