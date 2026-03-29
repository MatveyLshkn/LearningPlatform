package by.gsu.learningplatform.core.web;

import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PaginationUtils {

    private final LearningPlatformProperties.PaginationProperties paginationProperties;

    public PaginationUtils(LearningPlatformProperties properties) {
        this.paginationProperties = properties.pagination();
    }

    public Pageable toPageable(Integer limit) {
        var effectiveLimit = limit == null ? paginationProperties.defaultLimit() : limit;
        effectiveLimit = Math.min(effectiveLimit, paginationProperties.maxLimit());
        return PageRequest.of(0, effectiveLimit);
    }
}
