package com.gui.particles.comment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTests {

    @Test
    void mapsToCommentsTable() throws NoSuchFieldException {
        Table table = Comment.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("comments");

        Column articleId = Comment.class.getDeclaredField("articleId").getAnnotation(Column.class);
        Column authorId = Comment.class.getDeclaredField("authorId").getAnnotation(Column.class);
        Column parentCommentId = Comment.class.getDeclaredField("parentCommentId").getAnnotation(Column.class);
        Column body = Comment.class.getDeclaredField("body").getAnnotation(Column.class);
        Column deleted = Comment.class.getDeclaredField("deleted").getAnnotation(Column.class);
        Column createdAt = Comment.class.getDeclaredField("createdAt").getAnnotation(Column.class);
        Column editedAt = Comment.class.getDeclaredField("editedAt").getAnnotation(Column.class);

        assertThat(articleId.name()).isEqualTo("article_id");
        assertThat(authorId.name()).isEqualTo("author_id");
        assertThat(parentCommentId.name()).isEqualTo("parent_comment_id");
        assertThat(body.name()).isEqualTo("body");
        assertThat(deleted.name()).isEqualTo("deleted");
        assertThat(createdAt.name()).isEqualTo("created_at");
        assertThat(editedAt.name()).isEqualTo("edited_at");
    }

    @Test
    void createsTopLevelComment() {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Comment comment = Comment.createTopLevel(articleId, authorId, "Great article.");

        assertThat(comment.articleId()).isEqualTo(articleId);
        assertThat(comment.authorId()).isEqualTo(authorId);
        assertThat(comment.parentCommentId()).isNull();
        assertThat(comment.body()).isEqualTo("Great article.");
        assertThat(comment.isTopLevel()).isTrue();
        assertThat(comment.isReply()).isFalse();
        assertThat(comment.isDeleted()).isFalse();
        assertThat(comment.createdAt()).isNotNull();
        assertThat(comment.editedAt()).isNull();
    }

    @Test
    void createsReplyFromTopLevelParent() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID replierId = UUID.randomUUID();
        Comment parent = Comment.createTopLevel(articleId, UUID.randomUUID(), "Parent comment.");
        UUID parentId = UUID.randomUUID();
        setId(parent, parentId);

        Comment reply = parent.replyBy(replierId, "Reply body.");

        assertThat(reply.articleId()).isEqualTo(articleId);
        assertThat(reply.authorId()).isEqualTo(replierId);
        assertThat(reply.parentCommentId()).isEqualTo(parentId);
        assertThat(reply.body()).isEqualTo("Reply body.");
        assertThat(reply.isTopLevel()).isFalse();
        assertThat(reply.isReply()).isTrue();
    }

    @Test
    void rejectsReplyToReply() throws Exception {
        Comment parent = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Parent comment.");
        setId(parent, UUID.randomUUID());
        Comment reply = parent.replyBy(UUID.randomUUID(), "Reply body.");
        setId(reply, UUID.randomUUID());

        assertThatThrownBy(() -> reply.replyBy(UUID.randomUUID(), "Nested reply."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Replies can only target top-level comments");
    }

    @Test
    void rejectsReplyToDeletedComment() {
        Comment parent = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Parent comment.");
        parent.delete();

        assertThatThrownBy(() -> parent.replyBy(UUID.randomUUID(), "Reply body."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot reply to a deleted comment");
    }

    @Test
    void editsCommentBodyAndSetsEditedAt() {
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Original body.");

        comment.edit("Updated body.");

        assertThat(comment.body()).isEqualTo("Updated body.");
        assertThat(comment.editedAt()).isNotNull();
    }

    @Test
    void rejectsEditingDeletedComment() {
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Original body.");
        comment.delete();

        assertThatThrownBy(() -> comment.edit("Updated body."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot edit a deleted comment");
    }

    @Test
    void softDeletesComment() {
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Original body.");

        comment.delete();

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.body()).isEqualTo("Original body.");
    }

    @Test
    void requiresCommentFacts() {
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        assertThatThrownBy(() -> Comment.createTopLevel(null, authorId, "Body"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("articleId must not be null");
        assertThatThrownBy(() -> Comment.createTopLevel(articleId, null, "Body"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authorId must not be null");
        assertThatThrownBy(() -> Comment.createTopLevel(articleId, authorId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("body must not be null");
        assertThatThrownBy(() -> Comment.createTopLevel(articleId, authorId, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("body must not be blank");
    }

    private void setId(Comment comment, UUID id) throws Exception {
        Field idField = Comment.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(comment, id);
    }
}
