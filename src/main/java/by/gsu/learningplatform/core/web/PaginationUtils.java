package by.gsu.learningplatform.core.web;

import lombok.val;
import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import by.gsu.learningplatform.core.error.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PaginationUtils {

    private final LearningPlatformProperties.PaginationProperties paginationProperties;

    public PaginationUtils(final LearningPlatformProperties properties) {
        this.paginationProperties = properties.pagination();
    }

    public Pageable toPageable(final Integer limit) {
        return toPageable(limit, null);
    }

    public Pageable toPageable(final Integer limit, final String cursor) {
        var effectiveLimit = limit == null ? paginationProperties.defaultLimit() : limit;
        if (effectiveLimit <= 0) {
            throw new BadRequestException("Limit must be greater than zero");
        }
        effectiveLimit = Math.min(effectiveLimit, paginationProperties.maxLimit());
        return PageRequest.of(toPage(cursor), effectiveLimit);
    }

    public String nextCursor(final Page<?> page) {
        return page.hasNext() ? String.valueOf(page.getNumber() + 1) : null;
    }

    private int toPage(final String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            val page = Integer.parseInt(cursor);
            if (page < 0) {
                throw new BadRequestException("Cursor must not be negative");
            }
            return page;
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Cursor must be a page number");
        }
    }
}
