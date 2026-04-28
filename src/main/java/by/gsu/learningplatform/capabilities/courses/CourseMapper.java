package by.gsu.learningplatform.capabilities.courses;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "tags", expression = "java(toTagNames(entity))")
    CourseResponse toResponse(CourseEntity entity);

    default List<String> toTagNames(CourseEntity entity) {
        if (entity.getTags() == null) {
            return List.of();
        }
        return entity.getTags().stream()
                .map(TagEntity::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
