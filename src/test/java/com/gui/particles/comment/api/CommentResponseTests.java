package com.gui.particles.comment.api;

import com.gui.particles.comment.application.CommentThread;
import com.gui.particles.comment.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommentResponseTests {

    @Test
    void createCommentRequestRequiresBodyAndAllowsOptionalParent() throws NoSuchMethodException {
        RecordComponent parentCommentId = recordComponent(CreateCommentRequest.class, "parentCommentId");
        Method body = CreateCommentRequest.class.getDeclaredMethod("body");
        NotBlank notBlank = body.getAnnotation(NotBlank.class);
        Size size = body.getAnnotation(Size.class);

        assertThat(notBlank).isNotNull();
        assertThat(notBlank.message()).isEqualTo("Comment body is required");
        assertThat(size).isNotNull();
        assertThat(size.max()).isEqualTo(2000);
        assertThat(parentCommentId.getType()).isEqualTo(UUID.class);
    }

    @Test
    void updateCommentRequestRequiresBody() throws NoSuchMethodException {
        Method body = UpdateCommentRequest.class.getDeclaredMethod("body");
        NotBlank notBlank = body.getAnnotation(NotBlank.class);
        Size size = body.getAnnotation(Size.class);

        assertThat(notBlank).isNotNull();
        assertThat(notBlank.message()).isEqualTo("Comment body is required");
        assertThat(size).isNotNull();
        assertThat(size.max()).isEqualTo(2000);
    }

    @Test
    void mapsVisibleCommentToResponse() throws Exception {
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Comment comment = Comment.createTopLevel(articleId, authorId, "Visible body.");
        setField(comment, "id", commentId);

        CommentResponse response = CommentResponse.from(comment);

        assertThat(response.id()).isEqualTo(commentId);
        assertThat(response.articleId()).isEqualTo(articleId);
        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.parentCommentId()).isNull();
        assertThat(response.body()).isEqualTo("Visible body.");
        assertThat(response.deleted()).isFalse();
        assertThat(response.createdAt()).isEqualTo(comment.createdAt());
        assertThat(response.editedAt()).isNull();
    }

    @Test
    void hidesDeletedCommentBodyInResponse() {
        Comment comment = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Hidden body.");
        comment.delete();

        CommentResponse response = CommentResponse.from(comment);

        assertThat(response.body()).isNull();
        assertThat(response.deleted()).isTrue();
    }

    @Test
    void mapsThreadWithRepliesToNestedResponse() throws Exception {
        Comment parent = Comment.createTopLevel(UUID.randomUUID(), UUID.randomUUID(), "Parent body.");
        setField(parent, "id", UUID.randomUUID());
        Comment firstReply = parent.replyBy(UUID.randomUUID(), "First reply.");
        setField(firstReply, "id", UUID.randomUUID());
        Comment secondReply = parent.replyBy(UUID.randomUUID(), "Second reply.");
        setField(secondReply, "id", UUID.randomUUID());

        CommentThreadResponse response = CommentThreadResponse.from(new CommentThread(
                parent,
                List.of(firstReply, secondReply)
        ));

        assertThat(response.id()).isEqualTo(parent.id());
        assertThat(response.body()).isEqualTo("Parent body.");
        assertThat(response.replies())
                .extracting(CommentResponse::body)
                .containsExactly("First reply.", "Second reply.");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private RecordComponent recordComponent(Class<?> recordType, String name) {
        return Arrays.stream(recordType.getRecordComponents())
                .filter(component -> component.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
