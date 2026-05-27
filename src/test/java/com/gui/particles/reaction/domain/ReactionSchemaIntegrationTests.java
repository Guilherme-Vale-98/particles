package com.gui.particles.reaction.domain;

import com.gui.particles.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class ReactionSchemaIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void createsReactionsTableAndColumns() {
        assertThat(tableExists("reactions")).isTrue();

        assertThat(columnExists("reactions", "id")).isTrue();
        assertThat(columnExists("reactions", "user_id")).isTrue();
        assertThat(columnExists("reactions", "article_id")).isTrue();
        assertThat(columnExists("reactions", "type")).isTrue();
        assertThat(columnExists("reactions", "created_at")).isTrue();
        assertThat(columnExists("reactions", "updated_at")).isTrue();
    }

    @Test
    void createsUniqueConstraintForUserAndArticle() {
        assertThat(indexExists("reactions_user_article_key")).isTrue();
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
