package com.gui.particles.comment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByArticleIdAndParentCommentIdIsNullOrderByCreatedAtAsc(UUID articleId);

    List<Comment> findByParentCommentIdInOrderByCreatedAtAsc(Collection<UUID> parentCommentIds);

    Optional<Comment> findByIdAndArticleIdAndParentCommentIdIsNull(UUID id, UUID articleId);
}
