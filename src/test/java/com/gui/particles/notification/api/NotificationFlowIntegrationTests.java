package com.gui.particles.notification.api;

import com.gui.particles.AbstractIntegrationTest;
import com.gui.particles.article.domain.ArticleReactionCountRepository;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.article.domain.ArticleTagRepository;
import com.gui.particles.article.domain.ArticleVersionRepository;
import com.gui.particles.comment.domain.CommentRepository;
import com.gui.particles.friendship.domain.FriendshipRepository;
import com.gui.particles.notification.domain.Notification;
import com.gui.particles.notification.domain.NotificationRepository;
import com.gui.particles.notification.domain.NotificationType;
import com.gui.particles.reaction.domain.ReactionRepository;
import com.gui.particles.users.domain.IdentityProvider;
import com.gui.particles.users.domain.UserIdentity;
import com.gui.particles.users.domain.UserIdentityRepository;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationFlowIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private ArticleReactionCountRepository articleReactionCountRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleTagRepository articleTagRepository;

    @Autowired
    private ArticleVersionRepository articleVersionRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("delete from event_publication_archive").update();
        jdbcClient.sql("delete from event_publication").update();
        notificationRepository.deleteAll();
        commentRepository.deleteAll();
        reactionRepository.deleteAll();
        articleReactionCountRepository.deleteAll();
        jdbcClient.sql("delete from feed_items").update();
        friendshipRepository.deleteAll();
        articleTagRepository.deleteAll();
        articleVersionRepository.deleteAll();
        articleRepository.deleteAll();
        userIdentityRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void socialEventsCreateNotificationsAfterCommit() throws Exception {
        User alice = createLinkedUser("alice", "alice-sub");
        User bob = createLinkedUser("bob", "bob-sub");
        User carol = createLinkedUser("carol", "carol-sub");
        User dave = createLinkedUser("dave", "dave-sub");

        String friendshipId = sendFriendRequest(alice, bob);
        assertLatestNotification(
                bob.id(),
                alice.id(),
                NotificationType.FRIEND_REQUEST,
                UUID.fromString(friendshipId),
                null
        );
        mockMvc.perform(get("/api/v1/notifications")
                        .with(authenticatedAs(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("FRIEND_REQUEST"))
                .andExpect(jsonPath("$.items[0].actor.id").value(alice.id().toString()))
                .andExpect(jsonPath("$.items[0].actor.username").value("alice"))
                .andExpect(jsonPath("$.items[0].actionTargetId").value(friendshipId))
                .andExpect(jsonPath("$.items[0].article").doesNotExist())
                .andExpect(jsonPath("$.items[0].comment").doesNotExist());

        acceptFriendRequest(friendshipId, bob);
        assertLatestNotification(
                alice.id(),
                bob.id(),
                NotificationType.FRIEND_ACCEPTED,
                UUID.fromString(friendshipId),
                null
        );

        PublishedArticle article = createAndPublishArticle(alice);

        reactToArticle(article.slug(), bob);
        assertLatestNotification(
                alice.id(),
                bob.id(),
                NotificationType.ARTICLE_REACTION,
                article.id(),
                null
        );

        UUID topLevelCommentId = createComment(
                article.slug(),
                carol,
                """
                        {
                          "body": "This is a top-level comment."
                        }
                        """
        );
        assertLatestNotification(
                alice.id(),
                carol.id(),
                NotificationType.ARTICLE_COMMENT,
                article.id(),
                topLevelCommentId
        );

        UUID replyId = createComment(
                article.slug(),
                dave,
                """
                        {
                          "body": "This is a reply.",
                          "parentCommentId": "%s"
                        }
                        """.formatted(topLevelCommentId)
        );
        assertLatestNotification(
                carol.id(),
                dave.id(),
                NotificationType.COMMENT_REPLY,
                article.id(),
                replyId
        );
        mockMvc.perform(get("/api/v1/notifications")
                        .with(authenticatedAs(carol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("COMMENT_REPLY"))
                .andExpect(jsonPath("$.items[0].actor.id").value(dave.id().toString()))
                .andExpect(jsonPath("$.items[0].actor.username").value("dave"))
                .andExpect(jsonPath("$.items[0].article.id").value(article.id().toString()))
                .andExpect(jsonPath("$.items[0].article.slug").value(article.slug()))
                .andExpect(jsonPath("$.items[0].article.title").value("Notification Events In Practice"))
                .andExpect(jsonPath("$.items[0].comment.id").value(replyId.toString()))
                .andExpect(jsonPath("$.items[0].comment.body").value("This is a reply."))
                .andExpect(jsonPath("$.items[0].comment.deleted").value(false))
                .andExpect(jsonPath("$.items[0].actionTargetId").doesNotExist());
    }

    private String sendFriendRequest(User requester, User receiver) throws Exception {
        return JsonPath.read(
                mockMvc.perform(post("/api/v1/friendship-requests")
                                .with(authenticatedAs(requester))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "receiverId": "%s"
                                        }
                                        """.formatted(receiver.id())))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        );
    }

    private void acceptFriendRequest(String friendshipId, User receiver) throws Exception {
        mockMvc.perform(patch("/api/v1/friendship-requests/{id}", friendshipId)
                        .with(authenticatedAs(receiver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private PublishedArticle createAndPublishArticle(User author) throws Exception {
        String slug = JsonPath.read(
                mockMvc.perform(post("/api/v1/articles")
                                .with(authenticatedAs(author))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Notification Events In Practice",
                                          "summary": "A notification integration test.",
                                          "body": "This article receives reactions and comments so notifications can be tested.",
                                          "tags": ["notifications"]
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.slug"
        );

        String response = mockMvc.perform(post("/api/v1/articles/{slug}/publish", slug)
                        .with(authenticatedAs(author)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String articleId = JsonPath.read(response, "$.id");
        return new PublishedArticle(UUID.fromString(articleId), slug);
    }

    private void reactToArticle(String slug, User user) throws Exception {
        mockMvc.perform(post("/api/v1/articles/{slug}/reactions", slug)
                        .with(authenticatedAs(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "LIKE"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private UUID createComment(String slug, User user, String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/articles/{slug}/comments", slug)
                        .with(authenticatedAs(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = JsonPath.read(response, "$.id");
        return UUID.fromString(id);
    }

    private void assertLatestNotification(
            UUID recipientId,
            UUID actorId,
            NotificationType type,
            UUID referenceId,
            UUID secondaryReferenceId
    ) {
        List<Notification> notifications = notificationRepository.findLatestForRecipient(
                recipientId,
                PageRequest.of(0, 1)
        );

        assertThat(notifications).hasSize(1);
        Notification notification = notifications.getFirst();
        assertThat(notification.recipientId()).isEqualTo(recipientId);
        assertThat(notification.actorId()).isEqualTo(actorId);
        assertThat(notification.type()).isEqualTo(type);
        assertThat(notification.referenceId()).isEqualTo(referenceId);
        assertThat(notification.secondaryReferenceId()).isEqualTo(secondaryReferenceId);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.createdAt()).isNotNull();
        assertThat(notification.readAt()).isNull();
    }

    private User createLinkedUser(String username, String providerSubject) {
        UUID userId = UUID.randomUUID();
        userProfileRepository.save(UserProfile.create(
                userId,
                username,
                username,
                null,
                null
        ));
        userIdentityRepository.save(UserIdentity.create(
                userId,
                IdentityProvider.CUSTOM,
                providerSubject,
                username + "@example.com"
        ));
        return new User(userId, providerSubject);
    }

    private RequestPostProcessor authenticatedAs(User user) {
        return jwt().jwt(token -> token.subject(user.providerSubject()));
    }

    private record User(UUID id, String providerSubject) {
    }

    private record PublishedArticle(UUID id, String slug) {
    }
}
