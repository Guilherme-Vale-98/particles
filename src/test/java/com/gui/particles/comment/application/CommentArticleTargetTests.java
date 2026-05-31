package com.gui.particles.comment.application;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommentArticleTargetTests {

    @Test
    void carriesOnlyArticleFactsNeededForComments() {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        CommentArticleTarget target = new CommentArticleTarget(
                articleId,
                authorId,
                "published-article-a1b2c3d4"
        );

        assertThat(target.articleId()).isEqualTo(articleId);
        assertThat(target.authorId()).isEqualTo(authorId);
        assertThat(target.slug()).isEqualTo("published-article-a1b2c3d4");
    }
}
