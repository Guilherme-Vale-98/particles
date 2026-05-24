package com.gui.particles.article.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateArticleRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be 200 characters or less")
        String title,

        @Size(max = 500, message = "Summary must be 500 characters or less")
        String summary,

        @NotBlank(message = "Body is required")
        String body,

        @Size(max = 10, message = "An article can have at most 10 tags")
        List<@NotBlank(message = "Tag must not be blank")
        @Size(max = 50, message = "Tag must be 50 characters or less") String> tags
) {
}
