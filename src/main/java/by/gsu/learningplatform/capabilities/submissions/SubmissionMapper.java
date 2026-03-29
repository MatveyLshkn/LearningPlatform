package by.gsu.learningplatform.capabilities.submissions;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {

    SubmissionResponse toResponse(SubmissionEntity entity);
}
