package com.gui.particles.reaction.domain;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReactionRepositoryTests {

    @Test
    void extendsJpaRepositoryForReactionEntities() {
        assertThat(JpaRepository.class).isAssignableFrom(ReactionRepository.class);

        ParameterizedType repositoryType = (ParameterizedType) ReactionRepository.class.getGenericInterfaces()[0];

        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Reaction.class, UUID.class);
    }

    @Test
    void canFindReactionByUserAndArticle() throws NoSuchMethodException {
        Method method = ReactionRepository.class.getMethod("findByUserIdAndArticleId", UUID.class, UUID.class);
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();

        assertThat(returnType.getRawType()).isEqualTo(Optional.class);
        assertThat(returnType.getActualTypeArguments())
                .containsExactly(Reaction.class);
    }
}
