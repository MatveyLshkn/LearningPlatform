package by.gsu.learningplatform.capabilities.courses;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<TagEntity, UUID> {

    Optional<TagEntity> findByName(String name);

    List<TagEntity> findByNameIn(Collection<String> names);
}
