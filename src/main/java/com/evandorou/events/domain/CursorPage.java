package com.evandorou.events.domain;

import java.util.List;

/**
 * Cursor-based page of results.
 */
public record CursorPage<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {
    public static <T> CursorPage<T> of(List<T> items, String nextCursor) {
        boolean hasMore = nextCursor != null && !nextCursor.isBlank();
        return new CursorPage<>(items, nextCursor, hasMore);
    }
}
