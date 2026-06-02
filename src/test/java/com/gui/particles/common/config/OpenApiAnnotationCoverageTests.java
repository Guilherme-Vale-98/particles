package com.gui.particles.common.config;

import com.gui.particles.article.api.ArticleController;
import com.gui.particles.comment.api.CommentController;
import com.gui.particles.feed.api.FeedController;
import com.gui.particles.friendship.api.FriendshipController;
import com.gui.particles.notification.api.NotificationController;
import com.gui.particles.reaction.api.ReactionController;
import com.gui.particles.users.api.UserProfileController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiAnnotationCoverageTests {

    private final List<Class<?>> publicControllers = List.of(
            ArticleController.class,
            CommentController.class,
            FeedController.class,
            FriendshipController.class,
            NotificationController.class,
            ReactionController.class,
            UserProfileController.class
    );

    @Test
    void publicApiControllersHaveTags() {
        assertThat(publicControllers)
                .allSatisfy(controller -> assertThat(controller.getAnnotation(Tag.class))
                        .as(controller.getSimpleName() + " should have @Tag")
                        .isNotNull());
    }

    @Test
    void publicApiEndpointsHaveOperationDocumentation() {
        for (Class<?> controller : publicControllers) {
            for (Method method : controller.getDeclaredMethods()) {
                if (isEndpoint(method)) {
                    assertThat(method.getAnnotation(Operation.class))
                            .as(controller.getSimpleName() + "." + method.getName() + " should have @Operation")
                            .isNotNull();
                }
            }
        }
    }

    private boolean isEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(RequestMapping.class);
    }
}
