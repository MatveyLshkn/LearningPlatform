package by.gsu.learningplatform.core.web;

import java.util.List;
import java.util.Map;

public record CursorPageResponse<T>(List<T> items,
                                    PageMetadata page,
                                    Map<String, String> links) {

    public record PageMetadata(int limit,
                               int returned,
                               String nextCursor) {
    }
}
