package com.gui.particles.article.application;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;

@Component
public class SlugGenerator {

    private static final String FALLBACK_BASE = "article";
    private static final String SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 8;
    private static final int MAX_BASE_LENGTH = 80;

    private final SecureRandom random = new SecureRandom();

    public String generate(String title) {
        return base(title) + "-" + suffix();
    }

    private String base(String title) {
        if (!StringUtils.hasText(title)) {
            return FALLBACK_BASE;
        }

        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (!StringUtils.hasText(slug)) {
            return FALLBACK_BASE;
        }
        return trimTrailingDash(slug.substring(0, Math.min(slug.length(), MAX_BASE_LENGTH)));
    }

    private String trimTrailingDash(String slug) {
        return slug.replaceAll("-+$", "");
    }

    private String suffix() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(SUFFIX_ALPHABET.charAt(random.nextInt(SUFFIX_ALPHABET.length())));
        }
        return suffix.toString();
    }
}
