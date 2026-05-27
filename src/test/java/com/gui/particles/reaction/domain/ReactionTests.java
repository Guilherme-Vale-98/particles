package com.gui.particles.reaction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactionTests {

    @Test
    void mapsToReactionsTable() throws NoSuchFieldException {
        Table table = Reaction.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("reactions");

        Column userId = Reaction.class.getDeclaredField("userId").getAnnotation(Column.class);
        Column articleId = Reaction.class.getDeclaredField("articleId").getAnnotation(Column.class);
        Column type = Reaction.class.getDeclaredField("type").getAnnotation(Column.class);
        Column createdAt = Reaction.class.getDeclaredField("createdAt").getAnnotation(Column.class);
        Column updatedAt = Reaction.class.getDeclaredField("updatedAt").getAnnotation(Column.class);
        Enumerated typeMapping = Reaction.class.getDeclaredField("type").getAnnotation(Enumerated.class);

        assertThat(userId.name()).isEqualTo("user_id");
        assertThat(articleId.name()).isEqualTo("article_id");
        assertThat(type.name()).isEqualTo("type");
        assertThat(createdAt.name()).isEqualTo("created_at");
        assertThat(updatedAt.name()).isEqualTo("updated_at");
        assertThat(typeMapping.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void createsReactionForUserArticleAndType() {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();

        Reaction reaction = Reaction.create(userId, articleId, ReactionType.LIKE);

        assertThat(reaction.userId()).isEqualTo(userId);
        assertThat(reaction.articleId()).isEqualTo(articleId);
        assertThat(reaction.type()).isEqualTo(ReactionType.LIKE);
        assertThat(reaction.createdAt()).isNotNull();
        assertThat(reaction.updatedAt()).isNotNull();
    }

    @Test
    void changesReactionTypeAndRefreshesUpdatedAt() {
        Reaction reaction = Reaction.create(UUID.randomUUID(), UUID.randomUUID(), ReactionType.LIKE);

        reaction.changeType(ReactionType.CLAP);

        assertThat(reaction.type()).isEqualTo(ReactionType.CLAP);
        assertThat(reaction.updatedAt()).isAfterOrEqualTo(reaction.createdAt());
    }

    @Test
    void doesNotRefreshUpdatedAtWhenChangingToSameType() {
        Reaction reaction = Reaction.create(UUID.randomUUID(), UUID.randomUUID(), ReactionType.LIKE);

        reaction.changeType(ReactionType.LIKE);

        assertThat(reaction.type()).isEqualTo(ReactionType.LIKE);
        assertThat(reaction.updatedAt()).isEqualTo(reaction.createdAt());
    }

    @Test
    void requiresAllReactionFacts() {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();

        assertThatThrownBy(() -> Reaction.create(null, articleId, ReactionType.LIKE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("userId must not be null");
        assertThatThrownBy(() -> Reaction.create(userId, null, ReactionType.LIKE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("articleId must not be null");
        assertThatThrownBy(() -> Reaction.create(userId, articleId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
    }

    @Test
    void definesReactionTypes() {
        assertThat(ReactionType.values())
                .containsExactly(
                        ReactionType.LIKE,
                        ReactionType.INSIGHTFUL,
                        ReactionType.CLAP
                );
    }
}
