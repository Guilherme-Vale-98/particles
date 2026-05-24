package com.gui.particles.article.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugGeneratorTests {

    private final SlugGenerator slugGenerator = new SlugGenerator();

    @Test
    void createsTitleSlugWithShortRandomSuffix() {
        String slug = slugGenerator.generate("Hello, Spring Events!");

        assertThat(slug).matches("hello-spring-events-[a-z0-9]{8}");
    }

    @Test
    void normalizesAccentedCharacters() {
        String slug = slugGenerator.generate("Introducao a Java");

        assertThat(slug).matches("introducao-a-java-[a-z0-9]{8}");
    }

    @Test
    void fallsBackForBlankTitle() {
        String slug = slugGenerator.generate("   ");

        assertThat(slug).matches("article-[a-z0-9]{8}");
    }

    @Test
    void fallsBackForTitleWithoutSlugCharacters() {
        String slug = slugGenerator.generate("!!!");

        assertThat(slug).matches("article-[a-z0-9]{8}");
    }

    @Test
    void capsLongSlugBase() {
        String slug = slugGenerator.generate("a".repeat(120));

        assertThat(slug).matches("a{80}-[a-z0-9]{8}");
    }
}
