package com.gui.particles.article.api;

import com.gui.particles.AbstractIntegrationTest;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.article.domain.ArticleTagRepository;
import com.gui.particles.article.domain.ArticleVersionRepository;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArticleFlowIntegrationTests extends AbstractIntegrationTest {

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

    @BeforeEach
    void cleanDatabase() {
        articleTagRepository.deleteAll();
        articleVersionRepository.deleteAll();
        articleRepository.deleteAll();
        userIdentityRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void completesDraftUpdatePublishReadAndArchiveFlow() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");

        String createdArticle = mockMvc.perform(post("/api/v1/articles")
                        .with(authenticatedAs(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Events For Articles",
                                  "summary": "A practical event guide.",
                                  "body": "Spring events let modules react without tight coupling.",
                                  "tags": ["Spring", "Events"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/v1/articles/")))
                .andExpect(jsonPath("$.authorId").value(author.id().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.slug").isNotEmpty())
                .andExpect(jsonPath("$.tags[0]").value("spring"))
                .andExpect(jsonPath("$.tags[1]").value("events"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String slug = JsonPath.read(createdArticle, "$.slug");

        mockMvc.perform(get("/api/v1/articles/{slug}", slug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));

        mockMvc.perform(put("/api/v1/articles/{slug}", slug)
                        .with(authenticatedAs(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Events In Practice",
                                  "summary": "Updated summary.",
                                  "body": "Spring application events keep this article module focused.",
                                  "tags": ["spring", "architecture"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.title").value("Spring Events In Practice"))
                .andExpect(jsonPath("$.tags[1]").value("architecture"));

        mockMvc.perform(post("/api/v1/articles/{slug}/publish", slug)
                        .with(authenticatedAs(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").exists());

        mockMvc.perform(get("/api/v1/articles/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.body").value("Spring application events keep this article module focused."));

        mockMvc.perform(get("/api/v1/users/alice/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value(slug))
                .andExpect(jsonPath("$.items[0].body").doesNotExist())
                .andExpect(jsonPath("$.items[0].tags[0]").value("spring"))
                .andExpect(jsonPath("$.hasMore").value(false));

        mockMvc.perform(get("/api/v1/articles")
                        .queryParam("q", "application events")
                        .queryParam("tag", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value(slug))
                .andExpect(jsonPath("$.hasMore").value(false));

        mockMvc.perform(post("/api/v1/articles/{slug}/archive", slug)
                        .with(authenticatedAs(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/v1/articles/{slug}", slug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void rejectsNonAuthorUpdatesAndInvalidCreateRequests() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");
        User otherUser = createLinkedUser("bob", "bob-sub");
        String slug = JsonPath.read(
                mockMvc.perform(post("/api/v1/articles")
                                .with(authenticatedAs(author))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Author Only Changes",
                                          "summary": "Permissions matter.",
                                          "body": "Only the author can change an article.",
                                          "tags": ["security"]
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.slug"
        );

        mockMvc.perform(put("/api/v1/articles/{slug}", slug)
                        .with(authenticatedAs(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Trying To Edit",
                                  "summary": "Nope.",
                                  "body": "This should be rejected.",
                                  "tags": ["security"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(post("/api/v1/articles")
                        .with(authenticatedAs(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "summary": "Invalid.",
                                  "body": "",
                                  "tags": ["spring"]
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));
    }

    @Test
    void cursorPaginatesPublishedArticleCards() throws Exception {
        User author = createLinkedUser("alice", "alice-sub");
        publishArticle(author, "First Cursor Article", "first body about cursors");
        publishArticle(author, "Second Cursor Article", "second body about cursors");

        String firstPage = mockMvc.perform(get("/api/v1/users/alice/articles")
                        .queryParam("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(firstPage, "$.nextCursor");

        mockMvc.perform(get("/api/v1/users/alice/articles")
                        .queryParam("limit", "1")
                        .queryParam("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    private void publishArticle(User author, String title, String body) throws Exception {
        String slug = JsonPath.read(
                mockMvc.perform(post("/api/v1/articles")
                                .with(authenticatedAs(author))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "%s",
                                          "summary": "Summary.",
                                          "body": "%s",
                                          "tags": ["spring"]
                                        }
                                        """.formatted(title, body)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.slug"
        );

        mockMvc.perform(post("/api/v1/articles/{slug}/publish", slug)
                        .with(authenticatedAs(author)))
                .andExpect(status().isOk());
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
