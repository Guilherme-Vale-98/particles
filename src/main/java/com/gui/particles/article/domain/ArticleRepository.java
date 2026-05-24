package com.gui.particles.article.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Article> findByAuthorIdAndStatusOrderByPublishedAtDesc(UUID authorId, ArticleStatus status);

    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);

    List<ArticleCardProjection> findCardsByAuthorIdAndStatusOrderByPublishedAtDesc(
            UUID authorId,
            ArticleStatus status
    );

    List<ArticleCardProjection> findCardsByStatusOrderByPublishedAtDesc(ArticleStatus status);

    List<ArticleCardProjection> findCardsByIdInAndStatus(Collection<UUID> ids, ArticleStatus status);

    @Query(value = """
            select
                a.id as id,
                a.author_id as "authorId",
                a.title as title,
                a.slug as slug,
                a.summary as summary,
                a.status as status,
                a.read_time_minutes as "readTimeMinutes",
                a.view_count as "viewCount",
                a.published_at as "publishedAt",
                a.updated_at as "updatedAt"
            from articles a
            join article_tags t on t.article_id = a.id
            where a.status = 'PUBLISHED'
                and t.tag = :tag
            order by a.published_at desc, a.id desc
            """, nativeQuery = true)
    List<ArticleCardProjection> findPublishedCardsByTag(@Param("tag") String tag);

    @Query(value = """
            select
                a.id as id,
                a.author_id as "authorId",
                a.title as title,
                a.slug as slug,
                a.summary as summary,
                a.status as status,
                a.read_time_minutes as "readTimeMinutes",
                a.view_count as "viewCount",
                a.published_at as "publishedAt",
                a.updated_at as "updatedAt"
            from articles a
            where a.status = 'PUBLISHED'
                and to_tsvector(
                    'english',
                    coalesce(a.title, '') || ' ' || coalesce(a.summary, '') || ' ' || coalesce(a.body, '')
                ) @@ websearch_to_tsquery('english', :query)
            order by
                ts_rank_cd(
                    to_tsvector(
                        'english',
                        coalesce(a.title, '') || ' ' || coalesce(a.summary, '') || ' ' || coalesce(a.body, '')
                    ),
                    websearch_to_tsquery('english', :query)
                ) desc,
                a.published_at desc,
                a.id desc
            """, nativeQuery = true)
    List<ArticleCardProjection> searchPublishedCards(@Param("query") String query);

    @Query(value = """
            select
                a.id as id,
                a.author_id as "authorId",
                a.title as title,
                a.slug as slug,
                a.summary as summary,
                a.status as status,
                a.read_time_minutes as "readTimeMinutes",
                a.view_count as "viewCount",
                a.published_at as "publishedAt",
                a.updated_at as "updatedAt"
            from articles a
            join article_tags t on t.article_id = a.id
            where a.status = 'PUBLISHED'
                and t.tag = :tag
                and to_tsvector(
                    'english',
                    coalesce(a.title, '') || ' ' || coalesce(a.summary, '') || ' ' || coalesce(a.body, '')
                ) @@ websearch_to_tsquery('english', :query)
            order by
                ts_rank_cd(
                    to_tsvector(
                        'english',
                        coalesce(a.title, '') || ' ' || coalesce(a.summary, '') || ' ' || coalesce(a.body, '')
                    ),
                    websearch_to_tsquery('english', :query)
                ) desc,
                a.published_at desc,
                a.id desc
            """, nativeQuery = true)
    List<ArticleCardProjection> searchPublishedCardsByTag(
            @Param("query") String query,
            @Param("tag") String tag
    );

    @Modifying
    @Query("""
            update Article article
            set article.viewCount = article.viewCount + :delta
            where article.id = :articleId
            """)
    int incrementViewCount(@Param("articleId") UUID articleId, @Param("delta") long delta);
}
