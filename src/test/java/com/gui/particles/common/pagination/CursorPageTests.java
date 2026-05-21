package com.gui.particles.common.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CursorPageTests {

    @Test
    void createsPageWithNextCursorWhenMoreItemsExist() {
        CursorPage<String> page = CursorPage.of(List.of("a", "b"), "next-cursor", true);

        assertThat(page.items()).containsExactly("a", "b");
        assertThat(page.nextCursor()).isEqualTo("next-cursor");
        assertThat(page.hasMore()).isTrue();
    }

    @Test
    void createsLastPageWithoutNextCursor() {
        CursorPage<String> page = CursorPage.last(List.of("a"));

        assertThat(page.items()).containsExactly("a");
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasMore()).isFalse();
    }
}
