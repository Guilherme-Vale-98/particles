package com.gui.particles.notification.domain;

import com.gui.particles.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSchemaIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void createsNotificationsTableAndColumns() {
        assertThat(tableExists("notifications")).isTrue();

        assertThat(columnExists("notifications", "id")).isTrue();
        assertThat(columnExists("notifications", "recipient_id")).isTrue();
        assertThat(columnExists("notifications", "actor_id")).isTrue();
        assertThat(columnExists("notifications", "type")).isTrue();
        assertThat(columnExists("notifications", "reference_id")).isTrue();
        assertThat(columnExists("notifications", "secondary_reference_id")).isTrue();
        assertThat(columnExists("notifications", "read")).isTrue();
        assertThat(columnExists("notifications", "created_at")).isTrue();
        assertThat(columnExists("notifications", "read_at")).isTrue();
    }

    @Test
    void createsNotificationIndexes() {
        assertThat(indexExists("notifications_recipient_created_at_idx")).isTrue();
        assertThat(indexExists("notifications_recipient_read_created_at_idx")).isTrue();
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
