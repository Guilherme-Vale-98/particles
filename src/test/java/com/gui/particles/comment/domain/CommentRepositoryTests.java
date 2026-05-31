package com.gui.particles.comment.domain;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommentRepositoryTests {

    @Test
    void extendsJpaRepositoryForCommentEntities() {
        assertThat(JpaRepository.class).isAssignableFrom(CommentRepository.class);

        ParameterizedType repositoryType = (ParameterizedType) CommentRepository.class.getGenericInterfaces()[0];

        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Comment.class, UUID.class);
    }

    @Test
    void canLoadTopLevelCommentsForArticleInReadingOrder() throws NoSuchMethodException {
        Method method = CommentRepository.class.getMethod(
                "findByArticleIdAndParentCommentIdIsNullOrderByCreatedAtAsc",
                UUID.class
        );

        assertCommentListReturnType(method);
    }

    @Test
    void canLoadRepliesForMultipleParentsInReadingOrder() throws NoSuchMethodException {
        Method method = CommentRepository.class.getMethod(
                "findByParentCommentIdInOrderByCreatedAtAsc",
                Collection.class
        );

        assertCommentListReturnType(method);
    }

    @Test
    void canLoadTopLevelParentForArticle() throws NoSuchMethodException {
        Method method = CommentRepository.class.getMethod(
                "findByIdAndArticleIdAndParentCommentIdIsNull",
                UUID.class,
                UUID.class
        );
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(Optional.class);
        assertThat(returnType.getActualTypeArguments())
                .containsExactly(Comment.class);
    }

    private void assertCommentListReturnType(Method method) {
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(List.class);
        assertThat(returnType.getActualTypeArguments())
                .containsExactly(Comment.class);
    }
}
