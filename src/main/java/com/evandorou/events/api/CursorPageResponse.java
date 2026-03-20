package com.evandorou.events.api;

import com.evandorou.events.domain.CursorPage;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Cursor page wrapper. For `GET /api/v1/events`, `items` is an array of `EventResponse`.")
public record CursorPageResponse<T>(
        @ArraySchema(
                arraySchema = @Schema(description = "Items in this page (for list events: `EventResponse` objects)"),
                schema = @Schema(implementation = EventResponse.class)
        )
        List<T> items,
        @Schema(
                description = "Opaque cursor; pass as query param `cursor` to fetch the next page. Null or omitted when there is no next page.",
                nullable = true,
                example = "opaque-cursor-string"
        )
        String nextCursor,
        @Schema(description = "True if more results exist after this page", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static <T> CursorPageResponse<T> from(CursorPage<T> page) {
        return new CursorPageResponse<>(page.items(), page.nextCursor(), page.hasMore());
    }
}
