package com.gui.particles.article.domain;

import com.gui.particles.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleSchemaIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void createsArticleTables() {
        assertThat(tableExists("articles")).isTrue();
        assertThat(tableExists("article_tags")).isTrue();
        assertThat(tableExists("article_versions")).isTrue();
    }

    @Test
    void createsArticleColumns() {
        assertThat(columnExists("articles", "id")).isTrue();
        assertThat(columnExists("articles", "author_id")).isTrue();
        assertThat(columnExists("articles", "title")).isTrue();
        assertThat(columnExists("articles", "slug")).isTrue();
        assertThat(columnExists("articles", "summary")).isTrue();
        assertThat(columnExists("articles", "body")).isTrue();
        assertThat(columnExists("articles", "status")).isTrue();
        assertThat(columnExists("articles", "read_time_minutes")).isTrue();
        assertThat(columnExists("articles", "view_count")).isTrue();
        assertThat(columnExists("articles", "created_at")).isTrue();
        assertThat(columnExists("articles", "published_at")).isTrue();
        assertThat(columnExists("articles", "updated_at")).isTrue();
        assertThat(columnExists("articles", "version")).isTrue();
    }

    @Test
    void createsArticleTagColumns() {
        assertThat(columnExists("article_tags", "article_id")).isTrue();
        assertThat(columnExists("article_tags", "tag")).isTrue();
    }

    @Test
    void createsArticleVersionColumns() {
        assertThat(columnExists("article_versions", "id")).isTrue();
        assertThat(columnExists("article_versions", "article_id")).isTrue();
        assertThat(columnExists("article_versions", "body")).isTrue();
        assertThat(columnExists("article_versions", "edited_at")).isTrue();
    }

    @Test
    void createsArticleIndexes() {
        assertThat(indexExists("articles_author_status_published_at_idx")).isTrue();
        assertThat(indexExists("articles_slug_key")).isTrue();
        assertThat(indexExists("article_tags_tag_idx")).isTrue();
        assertThat(indexExists("articles_search_idx")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcClient.sql("""
                        select count(*)
                        from information_schema.tables
                        where table_schema = 'public'
                            and table_name = :tableName
                        """)
                .param("tableName", tableName)
                .query(Integer.class)
                .single();
        return count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcClient.sql("""
                        select count(*)
                        from information_schema.columns
                        where table_schema = 'public'
                            and table_name = :tableName
                            and column_name = :columnName
                        """)
                .param("tableName", tableName)
                .param("columnName", columnName)
                .query(Integer.class)
                .single();
        return count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcClient.sql("""
                        select count(*)
                        from pg_indexes
                        where schemaname = 'public'
                            and indexname = :indexName
                        """)
                .param("indexName", indexName)
                .query(Integer.class)
                .single();
        return count > 0;
    }
}
