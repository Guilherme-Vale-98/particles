package com.gui.particles.article.api;

import com.gui.particles.article.domain.Article;
import com.gui.particles.article.domain.ArticleCardProjection;
import com.gui.particles.article.domain.ArticleTag;
import com.gui.particles.article.domain.ArticleVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ArticleMapper {

    @Mapping(target = "id", expression = "java(article.id())")
    @Mapping(target = "authorId", expression = "java(article.authorId())")
    @Mapping(target = "title", expression = "java(article.title())")
    @Mapping(target = "slug", expression = "java(article.slug())")
    @Mapping(target = "summary", expression = "java(article.summary())")
    @Mapping(target = "body", expression = "java(article.body())")
    @Mapping(target = "status", expression = "java(article.status())")
    @Mapping(target = "readTimeMinutes", expression = "java(article.readTimeMinutes())")
    @Mapping(target = "viewCount", expression = "java(article.viewCount())")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagValues")
    @Mapping(target = "createdAt", expression = "java(article.createdAt())")
    @Mapping(target = "publishedAt", expression = "java(article.publishedAt())")
    @Mapping(target = "updatedAt", expression = "java(article.updatedAt())")
    @Mapping(target = "version", expression = "java(article.version())")
    ArticleResponse toResponse(Article article, List<ArticleTag> tags);

    @Mapping(target = "id", expression = "java(article.id())")
    @Mapping(target = "authorId", expression = "java(article.authorId())")
    @Mapping(target = "title", expression = "java(article.title())")
    @Mapping(target = "slug", expression = "java(article.slug())")
    @Mapping(target = "summary", expression = "java(article.summary())")
    @Mapping(target = "status", expression = "java(article.status())")
    @Mapping(target = "readTimeMinutes", expression = "java(article.readTimeMinutes())")
    @Mapping(target = "viewCount", expression = "java(article.viewCount())")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagValues")
    @Mapping(target = "reactionCounts", expression = "java(java.util.Map.of())")
    @Mapping(target = "publishedAt", expression = "java(article.publishedAt())")
    @Mapping(target = "updatedAt", expression = "java(article.updatedAt())")
    ArticleCardResponse toCardResponse(Article article, List<ArticleTag> tags);

    @Mapping(target = "id", source = "article.id")
    @Mapping(target = "authorId", source = "article.authorId")
    @Mapping(target = "title", source = "article.title")
    @Mapping(target = "slug", source = "article.slug")
    @Mapping(target = "summary", source = "article.summary")
    @Mapping(target = "status", source = "article.status")
    @Mapping(target = "readTimeMinutes", source = "article.readTimeMinutes")
    @Mapping(target = "viewCount", source = "article.viewCount")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "reactionCounts", source = "reactionCounts")
    @Mapping(target = "publishedAt", source = "article.publishedAt")
    @Mapping(target = "updatedAt", source = "article.updatedAt")
    ArticleCardResponse toCardResponse(
            ArticleCardProjection article,
            List<String> tags,
            Map<String, Long> reactionCounts
    );

    @Mapping(target = "id", expression = "java(articleVersion.id())")
    @Mapping(target = "articleId", expression = "java(articleVersion.articleId())")
    @Mapping(target = "body", expression = "java(articleVersion.body())")
    @Mapping(target = "editedAt", expression = "java(articleVersion.editedAt())")
    ArticleVersionResponse toResponse(ArticleVersion articleVersion);

    @Named("tagValues")
    default List<String> tagValues(List<ArticleTag> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(ArticleTag::tag)
                .toList();
    }
}
