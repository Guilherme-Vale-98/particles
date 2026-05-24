package com.gui.particles.article.application;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReadTimeCalculator {

    private static final int WORDS_PER_MINUTE = 200;

    public int calculate(String body) {
        if (!StringUtils.hasText(body)) {
            return 1;
        }

        int wordCount = body.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil((double) wordCount / WORDS_PER_MINUTE));
    }
}
