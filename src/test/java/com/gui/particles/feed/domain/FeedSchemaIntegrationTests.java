package com.gui.particles.feed.domain;

import com.gui.particles.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class FeedSchemaIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void createsFeedItemsTableAndRecipientCreatedAtIndex() {
        assertThat(tableExists("feed_items")).isTrue();

        assertThat(columnExists("feed_items", "id")).isTrue();
        assertThat(columnExists("feed_items", "recipient_id")).isTrue();
        assertThat(columnExists("feed_items", "article_id")).isTrue();
        assertThat(columnExists("feed_items", "author_id")).isTrue();
        assertThat(columnExists("feed_items", "created_at")).isTrue();

        assertThat(indexExists("feed_items_recipient_created_at_idx")).isTrue();
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
