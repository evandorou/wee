package com.evandorou.events.api;

import com.evandorou.events.domain.CursorPage;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {
    public static <T> CursorPageResponse<T> from(CursorPage<T> page) {
        return new CursorPageResponse<>(page.items(), page.nextCursor(), page.hasMore());
    }
}
