package com.gui.particles.reaction.api;

import com.gui.particles.common.error.DomainException;
import com.gui.particles.common.error.ErrorCode;
import com.gui.particles.common.error.GlobalExceptionHandler;
import com.gui.particles.reaction.application.ReactionService;
import com.gui.particles.reaction.domain.Reaction;
import com.gui.particles.reaction.domain.ReactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReactionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReactionService reactionService;

    @Test
    void reactsToArticle() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Reaction reaction = Reaction.create(userId, articleId, ReactionType.CLAP);
        when(reactionService.reactToArticle("article-slug", ReactionType.CLAP)).thenReturn(reaction);
        ReactToArticleRequest request = new ReactToArticleRequest(ReactionType.CLAP);

        mockMvc.perform(post("/api/v1/articles/{slug}/reactions", "article-slug")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.type").value("CLAP"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(reactionService).reactToArticle("article-slug", ReactionType.CLAP);
    }

    @Test
    void rejectsInvalidReactionBody() throws Exception {
        mockMvc.perform(post("/api/v1/articles/{slug}/reactions", "article-slug")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-failed"));

        verifyNoInteractions(reactionService);
    }

    @Test
    void deletesReaction() throws Exception {
        mockMvc.perform(delete("/api/v1/articles/{slug}/reactions", "article-slug"))
                .andExpect(status().isNoContent());

        verify(reactionService).deleteReaction("article-slug");
    }

    @Test
    void returnsProblemDetailForServiceErrors() throws Exception {
        ReactToArticleRequest request = new ReactToArticleRequest(ReactionType.LIKE);
        when(reactionService.reactToArticle("own-article", ReactionType.LIKE)).thenThrow(new DomainException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST,
                "Authors cannot react to their own articles"
        ));

        mockMvc.perform(post("/api/v1/articles/{slug}/reactions", "own-article")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad-request"))
                .andExpect(jsonPath("$.detail").value("Authors cannot react to their own articles"));
    }
}
