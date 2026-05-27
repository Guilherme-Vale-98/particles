package com.gui.particles.article.domain;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleRepositoryTests {

    @Test
    void articleRepositoryExtendsJpaRepositoryForArticleEntities() {
        assertThat(JpaRepository.class).isAssignableFrom(ArticleRepository.class);

        ParameterizedType repositoryType = (ParameterizedType) ArticleRepository.class.getGenericInterfaces()[0];

        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Article.class, UUID.class);
    }

    @Test
    void articleCardProjectionContainsOnlyCardFields() {
        assertThat(ArticleCardProjection.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactlyInAnyOrder(
                        "getId",
                        "getAuthorId",
                        "getTitle",
                        "getSlug",
                        "getSummary",
                        "getStatus",
                        "getReadTimeMinutes",
                        "getViewCount",
                        "getPublishedAt",
                        "getUpdatedAt"
                );
        assertThat(Arrays.stream(ArticleCardProjection.class.getMethods()).map(Method::getName))
                .doesNotContain("getBody");
    }

    @Test
    void articleRepositoryExposesCardProjectionQueries() throws NoSuchMethodException {
        assertProjectionListReturnType(ArticleRepository.class.getMethod(
                "findCardsByAuthorIdAndStatusOrderByPublishedAtDesc",
                UUID.class,
                ArticleStatus.class
        ));
        assertProjectionListReturnType(ArticleRepository.class.getMethod(
                "findCardsByStatusOrderByPublishedAtDesc",
                ArticleStatus.class
        ));
        assertProjectionListReturnType(ArticleRepository.class.getMethod(
                "findCardsByIdInAndStatus",
                Collection.class,
                ArticleStatus.class
        ));
        assertProjectionListReturnType(ArticleRepository.class.getMethod(
                "findPublishedCardsByTag",
                String.class
        ));
        assertProjectionListReturnType(ArticleRepository.class.getMethod(
                "searchPublishedCards",
                String.class
        ));
        assertProjectionListReturnType(ArticleRepository.class.getMethod(
                "searchPublishedCardsByTag",
                String.class,
                String.class
        ));
    }

    @Test
    void articleRepositorySearchUsesPostgreSqlFullTextSearchWithoutSelectingBody() throws NoSuchMethodException {
        Method method = ArticleRepository.class.getMethod("searchPublishedCards", String.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isTrue();
        assertThat(query.value())
                .contains("to_tsvector")
                .contains("websearch_to_tsquery")
                .contains("ts_rank_cd")
                .contains("a.body")
                .contains("a.status = 'PUBLISHED'");

        String selectClause = query.value().substring(0, query.value().indexOf("from articles"));
        assertThat(selectClause).doesNotContain("body");
    }

    @Test
    void articleRepositoryCanFilterPublishedCardsByTag() throws NoSuchMethodException {
        Method method = ArticleRepository.class.getMethod("findPublishedCardsByTag", String.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isTrue();
        assertThat(query.value())
                .contains("join article_tags t on t.article_id = a.id")
                .contains("a.status = 'PUBLISHED'")
                .contains("t.tag = :tag");
    }

    @Test
    void articleRepositoryCanSearchPublishedCardsByTagWithoutSelectingBody() throws NoSuchMethodException {
        Method method = ArticleRepository.class.getMethod("searchPublishedCardsByTag", String.class, String.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.nativeQuery()).isTrue();
        assertThat(query.value())
                .contains("join article_tags t on t.article_id = a.id")
                .contains("t.tag = :tag")
                .contains("to_tsvector")
                .contains("websearch_to_tsquery")
                .contains("a.body");

        String selectClause = query.value().substring(0, query.value().indexOf("from articles"));
        assertThat(selectClause).doesNotContain("body");
    }

    @Test
    void articleRepositoryCanIncrementViewCountAtomically() throws NoSuchMethodException {
        Method method = ArticleRepository.class.getMethod("incrementViewCount", UUID.class, long.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(method.getAnnotation(Modifying.class)).isNotNull();
        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("update Article article")
                .contains("article.viewCount = article.viewCount + :delta")
                .contains("where article.id = :articleId");
    }

    @Test
    void articleTagRepositoryExtendsJpaRepositoryForArticleTagEntities() {
        assertThat(JpaRepository.class).isAssignableFrom(ArticleTagRepository.class);

        ParameterizedType repositoryType = (ParameterizedType) ArticleTagRepository.class.getGenericInterfaces()[0];

        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(ArticleTag.class, ArticleTag.ArticleTagId.class);
    }

    @Test
    void articleTagRepositoryCanLoadTagsForMultipleArticles() throws NoSuchMethodException {
        Method method = ArticleTagRepository.class.getMethod("findByArticleIdIn", Collection.class);
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(List.class);
        assertThat(returnType.getActualTypeArguments())
                .containsExactly(ArticleTag.class);
    }

    @Test
    void articleReactionCountRepositoryExtendsJpaRepositoryForArticleReactionCountEntities() {
        assertThat(JpaRepository.class).isAssignableFrom(ArticleReactionCountRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType) ArticleReactionCountRepository.class.getGenericInterfaces()[0];

        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(
                        ArticleReactionCount.class,
                        ArticleReactionCount.ArticleReactionCountId.class
                );
    }

    @Test
    void articleReactionCountRepositoryCanLoadCountsForMultipleArticles() throws NoSuchMethodException {
        Method method = ArticleReactionCountRepository.class.getMethod("findByArticleIdIn", Collection.class);
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(List.class);
        assertThat(returnType.getActualTypeArguments())
                .containsExactly(ArticleReactionCount.class);
    }

    @Test
    void articleVersionRepositoryExtendsJpaRepositoryForArticleVersionEntities() {
        assertThat(JpaRepository.class).isAssignableFrom(ArticleVersionRepository.class);

        ParameterizedType repositoryType = (ParameterizedType) ArticleVersionRepository.class.getGenericInterfaces()[0];

        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(ArticleVersion.class, UUID.class);
    }

    private void assertProjectionListReturnType(Method method) {
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(List.class);
        assertThat(returnType.getActualTypeArguments())
                .containsExactly(ArticleCardProjection.class);
    }
}
