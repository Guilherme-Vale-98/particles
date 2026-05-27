package com.gui.particles.feed.api;

import com.gui.particles.AbstractIntegrationTest;
import com.gui.particles.article.domain.ArticleRepository;
import com.gui.particles.article.domain.ArticleTagRepository;
import com.gui.particles.article.domain.ArticleVersionRepository;
import com.gui.particles.feed.domain.FeedItemRepository;
import com.gui.particles.friendship.domain.FriendshipRepository;
import com.gui.particles.users.domain.IdentityProvider;
import com.gui.particles.users.domain.UserIdentity;
import com.gui.particles.users.domain.UserIdentityRepository;
import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedFlowIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private FeedItemRepository feedItemRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

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
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabaseAndRedis() {
        jdbcClient.sql("delete from event_publication_archive").update();
        jdbcClient.sql("delete from event_publication").update();
        feedItemRepository.deleteAll();
        friendshipRepository.deleteAll();
        articleTagRepository.deleteAll();
        articleVersionRepository.deleteAll();
        articleRepository.deleteAll();
        userIdentityRepository.deleteAll();
        userProfileRepository.deleteAll();

        Set<String> feedKeys = redisTemplate.keys("feed:*");
        if (feedKeys != null && !feedKeys.isEmpty()) {
            redisTemplate.delete(feedKeys);
        }
    }

    @Test
    void publishedArticleAppearsInFriendFeedAfterCommit() throws Exception {
        User alice = createLinkedUser("alice", "alice-sub");
        User bob = createLinkedUser("bob", "bob-sub");
        acceptRequest(alice, bob);

        String slug = createDraft(alice);
        publishArticle(alice, slug);

        mockMvc.perform(get("/api/v1/feed")
                        .with(authenticatedAs(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].authorId").value(alice.id().toString()))
                .andExpect(jsonPath("$.items[0].slug").value(slug))
                .andExpect(jsonPath("$.items[0].title").value("Feed Events In Practice"))
                .andExpect(jsonPath("$.items[0].summary").value("A feed integration test."))
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[0].tags[0]").value("feed"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void redisColdStartFallsBackToPostgresAndRewarmsRedis() throws Exception {
        User alice = createLinkedUser("alice", "alice-sub");
        User bob = createLinkedUser("bob", "bob-sub");
        acceptRequest(alice, bob);

        String slug = createDraft(alice);
        PublishedArticle article = publishArticle(alice, slug);
        String bobFeedKey = feedKey(bob);

        redisTemplate.delete(bobFeedKey);
        assertThat(redisTemplate.hasKey(bobFeedKey)).isFalse();

        mockMvc.perform(get("/api/v1/feed")
                        .with(authenticatedAs(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(article.id().toString()))
                .andExpect(jsonPath("$.items[0].slug").value(slug))
                .andExpect(jsonPath("$.items[0].authorId").value(alice.id().toString()))
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.hasMore").value(false));

        assertThat(redisTemplate.opsForZSet().score(bobFeedKey, article.id().toString())).isNotNull();
    }

    @Test
    void restoredArticleReappearsInFriendFeedWithoutNewFanout() throws Exception {
        User alice = createLinkedUser("alice", "alice-sub");
        User bob = createLinkedUser("bob", "bob-sub");
        acceptRequest(alice, bob);

        String slug = createDraft(alice);
        PublishedArticle article = publishArticle(alice, slug);
        assertThat(feedItemRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/articles/{slug}/archive", slug)
                        .with(authenticatedAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/v1/feed")
                        .with(authenticatedAs(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        mockMvc.perform(post("/api/v1/articles/{slug}/restore", slug)
                        .with(authenticatedAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/feed")
                        .with(authenticatedAs(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(article.id().toString()))
                .andExpect(jsonPath("$.items[0].slug").value(slug))
                .andExpect(jsonPath("$.hasMore").value(false));

        assertThat(feedItemRepository.count()).isEqualTo(1);
    }

    private void acceptRequest(User requester, User receiver) throws Exception {
        String friendshipId = JsonPath.read(
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

    private String createDraft(User author) throws Exception {
        return JsonPath.read(
                mockMvc.perform(post("/api/v1/articles")
                                .with(authenticatedAs(author))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Feed Events In Practice",
                                          "summary": "A feed integration test.",
                                          "body": "Publishing this article should fan out into a friend's feed.",
                                          "tags": ["feed"]
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("DRAFT"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.slug"
        );
    }

    private PublishedArticle publishArticle(User author, String slug) throws Exception {
        String response = mockMvc.perform(post("/api/v1/articles/{slug}/publish", slug)
                        .with(authenticatedAs(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String articleId = JsonPath.read(response, "$.id");
        return new PublishedArticle(UUID.fromString(articleId));
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

    private String feedKey(User user) {
        return "feed:" + user.id();
    }

    private record User(UUID id, String providerSubject) {
    }

    private record PublishedArticle(UUID id) {
    }
}
