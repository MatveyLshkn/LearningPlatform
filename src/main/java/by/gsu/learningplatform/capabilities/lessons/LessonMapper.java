package by.gsu.learningplatform.capabilities.lessons;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    LessonResponse toResponse(LessonEntity entity);
}
