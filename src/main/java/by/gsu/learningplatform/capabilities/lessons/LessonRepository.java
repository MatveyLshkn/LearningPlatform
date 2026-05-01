package by.gsu.learningplatform.capabilities.lessons;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<LessonEntity, UUID> {

    List<LessonEntity> findByCourseId(UUID courseId);
    Page<LessonEntity> findByCourseId(UUID courseId, Pageable pageable);
}
