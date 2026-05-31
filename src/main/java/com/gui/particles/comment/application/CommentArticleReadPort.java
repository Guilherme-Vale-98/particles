package com.gui.particles.comment.application;

public interface CommentArticleReadPort {

    CommentArticleTarget publishedArticleBySlug(String slug);
}
