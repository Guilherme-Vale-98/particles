package com.gui.particles.feed.api;

import com.gui.particles.article.api.ArticleCardResponse;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import com.gui.particles.feed.application.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Feed", description = "Read the current user's generated home feed.")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/feed")
    @Operation(summary = "Get current user feed", description = "Returns cursor-paginated article cards from the current user's Redis-backed feed with PostgreSQL fallback.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feed returned"),
            @ApiResponse(responseCode = "400", description = "Cursor or paging parameter is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public CursorPage<ArticleCardResponse> getCurrentUserFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return feedService.getCurrentUserFeed(cursor, limit);
    }
}
