package com.gui.particles.reaction.api;

import com.gui.particles.AbstractIntegrationTest;
import com.gui.particles.article.domain.ArticleReactionCountRepository;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.article.domain.ArticleTagRepository;
import com.gui.particles.article.domain.ArticleVersionRepository;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReactionFlowIntegrationTests extends AbstractIntegrationTest {

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
        reactionRepository.deleteAll();
        articleReactionCountRepository.deleteAll();
        articleTagRepository.deleteAll();
        articleVersionRepository.deleteAll();
        articleRepository.deleteAll();
        userIdentityRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void reactionFlowUpdatesArticleCardReactionCounts() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");
        User reader = createLinkedUser("bob", "bob-sub");
        String slug = createAndPublishArticle(author);

        mockMvc.perform(post("/api/v1/articles/{slug}/reactions", slug)
                        .with(authenticatedAs(reader))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "LIKE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("LIKE"))
                .andExpect(jsonPath("$.userId").value(reader.id().toString()));

        mockMvc.perform(get("/api/v1/users/alice/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value(slug))
                .andExpect(jsonPath("$.items[0].reactionCounts.LIKE").value(1));

        mockMvc.perform(post("/api/v1/articles/{slug}/reactions", slug)
                        .with(authenticatedAs(reader))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "CLAP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CLAP"));

        mockMvc.perform(get("/api/v1/users/alice/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reactionCounts.LIKE").doesNotExist())
                .andExpect(jsonPath("$.items[0].reactionCounts.CLAP").value(1));

        mockMvc.perform(delete("/api/v1/articles/{slug}/reactions", slug)
                        .with(authenticatedAs(reader)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/alice/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reactionCounts.CLAP").doesNotExist());
    }

    @Test
    void authorCannotReactToOwnArticle() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");
        String slug = createAndPublishArticle(author);

        mockMvc.perform(post("/api/v1/articles/{slug}/reactions", slug)
                        .with(authenticatedAs(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "LIKE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad-request"))
                .andExpect(jsonPath("$.detail").value("Authors cannot react to their own articles"));
    }

    private String createAndPublishArticle(User author) throws Exception {
        String slug = JsonPath.read(
                mockMvc.perform(post("/api/v1/articles")
                                .with(authenticatedAs(author))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Reaction Counts In Practice",
                                          "summary": "An integration test.",
                                          "body": "Reactions should update article card counts through events.",
                                          "tags": ["reactions"]
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.slug"
        );

        mockMvc.perform(post("/api/v1/articles/{slug}/publish", slug)
                        .with(authenticatedAs(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        return slug;
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
}
