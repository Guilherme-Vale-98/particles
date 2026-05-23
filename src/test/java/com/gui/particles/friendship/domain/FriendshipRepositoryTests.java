package com.gui.particles.friendship.domain;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.ParameterizedType;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FriendshipRepositoryTests {

    @Test
    void extendsJpaRepositoryForFriendshipEntities() {
        assertThat(JpaRepository.class).isAssignableFrom(FriendshipRepository.class);

        ParameterizedType repositoryType = (ParameterizedType) FriendshipRepository.class.getGenericInterfaces()[0];

        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Friendship.class, UUID.class);
    }
}
