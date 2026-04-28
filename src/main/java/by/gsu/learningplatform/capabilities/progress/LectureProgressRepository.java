package by.gsu.learningplatform.capabilities.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LectureProgressRepository extends JpaRepository<LectureProgressEntity, UUID> {

    Optional<LectureProgressEntity> findByUserIdAndLectureId(UUID userId, UUID lectureId);

    List<LectureProgressEntity> findByUserIdAndLectureIdIn(UUID userId, Collection<UUID> lectureIds);

    long countByUserIdAndLectureIdIn(UUID userId, Collection<UUID> lectureIds);
}
