package com.gui.particles.comment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "Comment body is required")
        @Size(max = 2000, message = "Comment body must be 2000 characters or less")
        String body
) {
}
