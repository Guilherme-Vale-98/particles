package com.gui.particles.feed.api;

import com.gui.particles.article.api.ArticleCardResponse;
import com.gui.particles.article.domain.ArticleStatus;
import com.gui.particles.common.error.GlobalExceptionHandler;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.feed.application.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FeedControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @Test
    void getsCurrentUserFeed() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        ArticleCardResponse article = new ArticleCardResponse(
                articleId,
                authorId,
                "A feed article",
                "a-feed-article-a1b2c3d4",
                "A useful summary",
                ArticleStatus.PUBLISHED,
                4,
                12,
                List.of("spring", "feed"),
                Instant.parse("2026-05-24T12:00:00Z"),
                Instant.parse("2026-05-24T12:30:00Z")
        );
        when(feedService.getCurrentUserFeed("cursor-1", 10))
                .thenReturn(CursorPage.of(List.of(article), "cursor-2", true));

        mockMvc.perform(get("/api/v1/feed")
                        .queryParam("cursor", "cursor-1")
                        .queryParam("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(articleId.toString()))
                .andExpect(jsonPath("$.items[0].authorId").value(authorId.toString()))
                .andExpect(jsonPath("$.items[0].title").value("A feed article"))
                .andExpect(jsonPath("$.items[0].slug").value("a-feed-article-a1b2c3d4"))
                .andExpect(jsonPath("$.items[0].summary").value("A useful summary"))
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[0].readTimeMinutes").value(4))
                .andExpect(jsonPath("$.items[0].viewCount").value(12))
                .andExpect(jsonPath("$.items[0].tags[0]").value("spring"))
                .andExpect(jsonPath("$.items[0].tags[1]").value("feed"))
                .andExpect(jsonPath("$.nextCursor").value("cursor-2"))
                .andExpect(jsonPath("$.hasMore").value(true));

        verify(feedService).getCurrentUserFeed("cursor-1", 10);
    }

    @Test
    void usesDefaultLimitWhenLimitIsNotProvided() throws Exception {
        when(feedService.getCurrentUserFeed(null, 20)).thenReturn(CursorPage.last(List.of()));

        mockMvc.perform(get("/api/v1/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(feedService).getCurrentUserFeed(null, 20);
    }
}
