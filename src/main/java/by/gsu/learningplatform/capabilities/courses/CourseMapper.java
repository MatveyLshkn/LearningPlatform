package by.gsu.learningplatform.capabilities.courses;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    CourseResponse toResponse(CourseEntity entity);
}
