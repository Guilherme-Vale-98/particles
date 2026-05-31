package com.gui.particles.comment.domain;

import com.gui.particles.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class CommentSchemaIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void createsCommentsTableAndColumns() {
        assertThat(tableExists("comments")).isTrue();

        assertThat(columnExists("comments", "id")).isTrue();
        assertThat(columnExists("comments", "article_id")).isTrue();
        assertThat(columnExists("comments", "author_id")).isTrue();
        assertThat(columnExists("comments", "parent_comment_id")).isTrue();
        assertThat(columnExists("comments", "body")).isTrue();
        assertThat(columnExists("comments", "deleted")).isTrue();
        assertThat(columnExists("comments", "created_at")).isTrue();
        assertThat(columnExists("comments", "edited_at")).isTrue();
    }

    @Test
    void createsCommentIndexes() {
        assertThat(indexExists("comments_article_parent_created_at_idx")).isTrue();
        assertThat(indexExists("comments_parent_created_at_idx")).isTrue();
        assertThat(indexExists("comments_author_created_at_idx")).isTrue();
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
