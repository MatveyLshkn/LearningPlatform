package by.gsu.learningplatform.capabilities.lectures;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LectureRepository extends JpaRepository<LectureEntity, UUID> {

    List<LectureEntity> findByLessonId(UUID lessonId);
}
