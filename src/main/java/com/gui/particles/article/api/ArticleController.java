package com.gui.particles.article.api;

import com.gui.particles.article.application.ArticleService;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Articles", description = "Create, publish, archive, restore, read, and search articles.")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/articles")
    @Operation(summary = "Create a draft article", description = "Creates a draft article for the current authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft article created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        ArticleResponse article = articleService.createDraft(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{slug}")
                .buildAndExpand(article.slug())
                .toUri();
        return ResponseEntity.created(location).body(article);
    }

    @GetMapping("/articles/{slug}")
    @Operation(summary = "Get a published article", description = "Returns a published article by slug and records an asynchronous view-count increment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published article returned"),
            @ApiResponse(responseCode = "404", description = "Article is missing or not public"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ArticleResponse getArticleBySlug(@PathVariable String slug) {
        return articleService.getPublishedArticleBySlug(slug);
    }

    @GetMapping("/articles")
    @Operation(summary = "Search published article cards", description = "Returns cursor-paginated published article cards, optionally filtered by tag or full-text query.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published article cards returned"),
            @ApiResponse(responseCode = "400", description = "Cursor or paging parameter is invalid"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public CursorPage<ArticleCardResponse> getArticles(
            @RequestParam(required = false) String tag,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return articleService.searchPublishedArticles(tag, query, cursor, limit);
    }

    @GetMapping("/users/{username}/articles")
    @Operation(summary = "List a user's published articles", description = "Returns cursor-paginated published article cards for a public user profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published article cards returned"),
            @ApiResponse(responseCode = "400", description = "Cursor or paging parameter is invalid"),
            @ApiResponse(responseCode = "404", description = "User profile not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public CursorPage<ArticleCardResponse> getArticlesByUsername(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return articleService.getPublishedArticlesByUsername(username, cursor, limit);
    }

    @PutMapping("/articles/{slug}")
    @Operation(summary = "Update an article", description = "Updates an article owned by the current user and stores the previous body as a retained version when the body changes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Current user is not the article author"),
            @ApiResponse(responseCode = "404", description = "Article not found"),
            @ApiResponse(responseCode = "409", description = "Article cannot be edited in its current state"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ArticleResponse updateArticle(
            @PathVariable String slug,
            @Valid @RequestBody UpdateArticleRequest request
    ) {
        return articleService.updateArticle(slug, request);
    }

    @PostMapping("/articles/{slug}/publish")
    @Operation(summary = "Publish a draft article", description = "Publishes an article owned by the current user and emits the article-published event after the transaction commits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article published"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Current user is not the article author"),
            @ApiResponse(responseCode = "404", description = "Article not found"),
            @ApiResponse(responseCode = "409", description = "Article is not publishable"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ArticleResponse publishArticle(@PathVariable String slug) {
        return articleService.publishArticle(slug);
    }

    @PostMapping("/articles/{slug}/archive")
    @Operation(summary = "Archive a published article", description = "Archives a published article owned by the current user so it disappears from public reads and cards.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article archived"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Current user is not the article author"),
            @ApiResponse(responseCode = "404", description = "Article not found"),
            @ApiResponse(responseCode = "409", description = "Article is not archivable"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ArticleResponse archiveArticle(@PathVariable String slug) {
        return articleService.archiveArticle(slug);
    }

    @PostMapping("/articles/{slug}/restore")
    @Operation(summary = "Restore an archived article", description = "Restores an archived article to published status without emitting a new publish event or changing its original published time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article restored"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Current user is not the article author"),
            @ApiResponse(responseCode = "404", description = "Article not found"),
            @ApiResponse(responseCode = "409", description = "Article is not restorable"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ArticleResponse restoreArticle(@PathVariable String slug) {
        return articleService.restoreArticle(slug);
    }
}
