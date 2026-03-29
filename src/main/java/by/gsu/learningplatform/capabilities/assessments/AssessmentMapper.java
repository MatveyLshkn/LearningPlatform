package by.gsu.learningplatform.capabilities.assessments;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssessmentMapper {

    AssessmentResponse toResponse(AssessmentEntity entity);
}
