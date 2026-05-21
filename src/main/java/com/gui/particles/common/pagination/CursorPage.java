package com.gui.particles.common.pagination;

import java.util.List;

public record CursorPage<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {

    public static <T> CursorPage<T> of(List<T> items, String nextCursor, boolean hasMore) {
        return new CursorPage<>(List.copyOf(items), nextCursor, hasMore);
    }

    public static <T> CursorPage<T> last(List<T> items) {
        return new CursorPage<>(List.copyOf(items), null, false);
    }
}
