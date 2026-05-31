package com.gui.particles.comment.api;

import com.gui.particles.AbstractIntegrationTest;
import com.gui.particles.users.domain.IdentityProvider;
import com.gui.particles.users.domain.UserIdentity;
import com.gui.particles.users.domain.UserIdentityRepository;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class CommentFlowIntegrationTests extends AbstractIntegrationTest {

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
        jdbcClient.sql("delete from comments").update();
        jdbcClient.sql("delete from reactions").update();
        jdbcClient.sql("delete from article_reaction_counts").update();
        jdbcClient.sql("delete from feed_items").update();
        jdbcClient.sql("delete from article_tags").update();
        jdbcClient.sql("delete from article_versions").update();
        jdbcClient.sql("delete from articles").update();
        jdbcClient.sql("delete from friendships").update();
        userIdentityRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void createsRepliesEditsDeletesAndPreservesRepliesUnderDeletedParent() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");
        User commenter = createLinkedUser("bob", "bob-sub");
        User otherUser = createLinkedUser("carol", "carol-sub");
        String slug = createAndPublishArticle(author, "Comment Flow Article");

        UUID topLevelCommentId = createComment(
                slug,
                commenter,
                """
                        {
                          "body": "This is a top-level comment."
                        }
                        """
        );

        UUID replyId = createComment(
                slug,
                otherUser,
                """
                        {
                          "body": "This is a reply.",
                          "parentCommentId": "%s"
                        }
                        """.formatted(topLevelCommentId)
        );

        mockMvc.perform(get("/api/v1/articles/{slug}/comments", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(topLevelCommentId.toString()))
                .andExpect(jsonPath("$.items[0].body").value("This is a top-level comment."))
                .andExpect(jsonPath("$.items[0].replies[0].id").value(replyId.toString()))
                .andExpect(jsonPath("$.items[0].replies[0].body").value("This is a reply."))
                .andExpect(jsonPath("$.hasMore").value(false));

        mockMvc.perform(put("/api/v1/comments/{id}", topLevelCommentId)
                        .with(authenticatedAs(commenter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Edited comment body."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Edited comment body."))
                .andExpect(jsonPath("$.editedAt").exists());

        mockMvc.perform(put("/api/v1/comments/{id}", topLevelCommentId)
                        .with(authenticatedAs(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Trying to edit someone else's comment."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(delete("/api/v1/comments/{id}", topLevelCommentId)
                        .with(authenticatedAs(commenter)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/articles/{slug}/comments", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(topLevelCommentId.toString()))
                .andExpect(jsonPath("$.items[0].deleted").value(true))
                .andExpect(jsonPath("$.items[0].body").value(nullValue()))
                .andExpect(jsonPath("$.items[0].replies[0].id").value(replyId.toString()))
                .andExpect(jsonPath("$.items[0].replies[0].body").value("This is a reply."));
    }

    @Test
    void rejectsReplyToReply() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");
        User commenter = createLinkedUser("bob", "bob-sub");
        String slug = createAndPublishArticle(author, "Reply To Reply Article");
        UUID topLevelCommentId = createComment(
                slug,
                commenter,
                """
                        {
                          "body": "Top-level comment."
                        }
                        """
        );
        UUID replyId = createComment(
                slug,
                commenter,
                """
                        {
                          "body": "First-level reply.",
                          "parentCommentId": "%s"
                        }
                        """.formatted(topLevelCommentId)
        );

        mockMvc.perform(post("/api/v1/articles/{slug}/comments", slug)
                        .with(authenticatedAs(commenter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Second-level reply should be rejected.",
                                  "parentCommentId": "%s"
                                }
                                """.formatted(replyId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"))
                .andExpect(jsonPath("$.detail").value("Parent comment not found"));
    }

    @Test
    void rejectsCommentsForDraftAndArchivedArticles() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");
        User commenter = createLinkedUser("bob", "bob-sub");
        String draftSlug = createDraftArticle(author, "Draft Comment Article");

        mockMvc.perform(post("/api/v1/articles/{slug}/comments", draftSlug)
                        .with(authenticatedAs(commenter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Draft articles should not accept comments."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));

        String archivedSlug = createAndPublishArticle(author, "Archived Comment Article");

        mockMvc.perform(post("/api/v1/articles/{slug}/archive", archivedSlug)
                        .with(authenticatedAs(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(post("/api/v1/articles/{slug}/comments", archivedSlug)
                        .with(authenticatedAs(commenter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Archived articles should not accept comments."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void cursorPaginatesTopLevelComments() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");
        User commenter = createLinkedUser("bob", "bob-sub");
        String slug = createAndPublishArticle(author, "Paginated Comments Article");
        UUID firstCommentId = createComment(
                slug,
                commenter,
                """
                        {
                          "body": "First top-level comment."
                        }
                        """
        );
        UUID secondCommentId = createComment(
                slug,
                commenter,
                """
                        {
                          "body": "Second top-level comment."
                        }
                        """
        );

        String firstPage = mockMvc.perform(get("/api/v1/articles/{slug}/comments", slug)
                        .queryParam("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(firstCommentId.toString()))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(firstPage, "$.nextCursor");

        mockMvc.perform(get("/api/v1/articles/{slug}/comments", slug)
                        .queryParam("limit", "1")
                        .queryParam("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(secondCommentId.toString()))
                .andExpect(jsonPath("$.hasMore").value(false));
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

    private String createAndPublishArticle(User author, String title) throws Exception {
        String slug = createDraftArticle(author, title);

        mockMvc.perform(post("/api/v1/articles/{slug}/publish", slug)
                        .with(authenticatedAs(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        return slug;
    }

    private String createDraftArticle(User author, String title) throws Exception {
        return JsonPath.read(
                mockMvc.perform(post("/api/v1/articles")
                                .with(authenticatedAs(author))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "%s",
                                          "summary": "Comments integration test.",
                                          "body": "This article exists so comments can be tested through the public API.",
                                          "tags": ["comments"]
                                        }
                                        """.formatted(title)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.slug"
        );
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

    @TestConfiguration
    static class LocalSecurityTestConfiguration {

        @Bean
        @Primary
        JwtDecoder googleJwtDecoder() {
            return LocalSecurityTestConfiguration::jwt;
        }

        @Bean
        @Primary
        JwtDecoder customJwtDecoder() {
            return LocalSecurityTestConfiguration::jwt;
        }

        @Bean
        @Primary
        OpaqueTokenIntrospector opaqueTokenIntrospector() {
            return token -> (OAuth2AuthenticatedPrincipal) new org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal(
                    "github-user",
                    Map.of("id", token),
                    java.util.List.of()
            );
        }

        private static Jwt jwt(String token) {
            Instant issuedAt = Instant.now();
            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject(token)
                    .issuedAt(issuedAt)
                    .expiresAt(issuedAt.plusSeconds(3600))
                    .claim("roles", java.util.List.of("USER"))
                    .build();
        }
    }
}
