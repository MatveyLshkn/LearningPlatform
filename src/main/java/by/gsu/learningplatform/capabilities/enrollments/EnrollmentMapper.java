package by.gsu.learningplatform.capabilities.enrollments;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    EnrollmentResponse toResponse(EnrollmentEntity entity);
}
