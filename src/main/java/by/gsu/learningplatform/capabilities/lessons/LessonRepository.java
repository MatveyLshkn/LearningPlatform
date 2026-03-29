package by.gsu.learningplatform.capabilities.lessons;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<LessonEntity, UUID> {

    List<LessonEntity> findByCourseId(UUID courseId);
}
