package com.gui.particles.article.api;

import com.gui.particles.article.application.ArticleService;
import com.gui.particles.common.pagination.CursorPage;
import com.gui.particles.common.pagination.CursorRequest;
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
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/articles")
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        ArticleResponse article = articleService.createDraft(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{slug}")
                .buildAndExpand(article.slug())
                .toUri();
        return ResponseEntity.created(location).body(article);
    }

    @GetMapping("/articles/{slug}")
    public ArticleResponse getArticleBySlug(@PathVariable String slug) {
        return articleService.getPublishedArticleBySlug(slug);
    }

    @GetMapping("/articles")
    public CursorPage<ArticleCardResponse> getArticles(
            @RequestParam(required = false) String tag,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return articleService.searchPublishedArticles(tag, query, cursor, limit);
    }

    @GetMapping("/users/{username}/articles")
    public CursorPage<ArticleCardResponse> getArticlesByUsername(
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + CursorRequest.DEFAULT_LIMIT) Integer limit
    ) {
        return articleService.getPublishedArticlesByUsername(username, cursor, limit);
    }

    @PutMapping("/articles/{slug}")
    public ArticleResponse updateArticle(
            @PathVariable String slug,
            @Valid @RequestBody UpdateArticleRequest request
    ) {
        return articleService.updateArticle(slug, request);
    }

    @PostMapping("/articles/{slug}/publish")
    public ArticleResponse publishArticle(@PathVariable String slug) {
        return articleService.publishArticle(slug);
    }

    @PostMapping("/articles/{slug}/archive")
    public ArticleResponse archiveArticle(@PathVariable String slug) {
        return articleService.archiveArticle(slug);
    }
}
