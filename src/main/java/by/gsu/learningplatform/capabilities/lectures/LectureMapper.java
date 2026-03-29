package by.gsu.learningplatform.capabilities.lectures;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LectureMapper {

    LectureResponse toResponse(LectureEntity entity);
}
