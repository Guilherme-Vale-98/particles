package com.gui.particles.friendship.domain;

import com.gui.particles.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendshipSchemaIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void createsFriendshipTableAndIndexes() {
        assertThat(tableExists("friendships")).isTrue();

        assertThat(columnExists("friendships", "id")).isTrue();
        assertThat(columnExists("friendships", "requester_id")).isTrue();
        assertThat(columnExists("friendships", "receiver_id")).isTrue();
        assertThat(columnExists("friendships", "user_low_id")).isTrue();
        assertThat(columnExists("friendships", "user_high_id")).isTrue();
        assertThat(columnExists("friendships", "status")).isTrue();
        assertThat(columnExists("friendships", "created_at")).isTrue();
        assertThat(columnExists("friendships", "responded_at")).isTrue();

        assertThat(indexExists("friendships_requester_id_idx")).isTrue();
        assertThat(indexExists("friendships_receiver_id_idx")).isTrue();
        assertThat(indexExists("friendships_user_low_high_idx")).isTrue();
        assertThat(indexExists("friendships_active_pair_key")).isTrue();
    }

    @Test
    void allowsOnlyOneActiveFriendshipPerCanonicalPair() {
        UUID requesterId = insertUser("requester-" + UUID.randomUUID());
        UUID receiverId = insertUser("receiver-" + UUID.randomUUID());
        UUID userLowId = requesterId.compareTo(receiverId) < 0 ? requesterId : receiverId;
        UUID userHighId = requesterId.compareTo(receiverId) < 0 ? receiverId : requesterId;

        insertFriendship(requesterId, receiverId, userLowId, userHighId, "PENDING");

        assertThatThrownBy(() -> insertFriendship(receiverId, requesterId, userLowId, userHighId, "ACCEPTED"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectedFriendshipDoesNotBlockFutureActiveFriendshipForSamePair() {
        UUID requesterId = insertUser("requester-" + UUID.randomUUID());
        UUID receiverId = insertUser("receiver-" + UUID.randomUUID());
        UUID userLowId = requesterId.compareTo(receiverId) < 0 ? requesterId : receiverId;
        UUID userHighId = requesterId.compareTo(receiverId) < 0 ? receiverId : requesterId;

        insertFriendship(requesterId, receiverId, userLowId, userHighId, "REJECTED");
        insertFriendship(receiverId, requesterId, userLowId, userHighId, "PENDING");

        Integer count = jdbcClient.sql("""
                        select count(*)
                        from friendships
                        where user_low_id = :userLowId
                            and user_high_id = :userHighId
                        """)
                .param("userLowId", userLowId)
                .param("userHighId", userHighId)
                .query(Integer.class)
                .single();

        assertThat(count).isEqualTo(2);
    }

    private UUID insertUser(String username) {
        UUID userId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into user_profiles (id, username, display_name)
                        values (:userId, :username, :displayName)
                        """)
                .param("userId", userId)
                .param("username", username)
                .param("displayName", username)
                .update();
        return userId;
    }

    private void insertFriendship(
            UUID requesterId,
            UUID receiverId,
            UUID userLowId,
            UUID userHighId,
            String status
    ) {
        jdbcClient.sql("""
                        insert into friendships (requester_id, receiver_id, user_low_id, user_high_id, status)
                        values (:requesterId, :receiverId, :userLowId, :userHighId, :status)
                        """)
                .param("requesterId", requesterId)
                .param("receiverId", receiverId)
                .param("userLowId", userLowId)
                .param("userHighId", userHighId)
                .param("status", status)
                .update();
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
