package com.gui.particles.article.application;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReadTimeCalculatorTests {

    private final ReadTimeCalculator calculator = new ReadTimeCalculator();

    @Test
    void blankBodyReturnsOneMinute() {
        assertThat(calculator.calculate(null)).isEqualTo(1);
        assertThat(calculator.calculate("")).isEqualTo(1);
        assertThat(calculator.calculate("   ")).isEqualTo(1);
    }

    @Test
    void shortBodyReturnsOneMinute() {
        assertThat(calculator.calculate("one two three")).isEqualTo(1);
    }

    @Test
    void exactlyTwoHundredWordsReturnsOneMinute() {
        assertThat(calculator.calculate(words(200))).isEqualTo(1);
    }

    @Test
    void moreThanTwoHundredWordsRoundsUp() {
        assertThat(calculator.calculate(words(201))).isEqualTo(2);
    }

    @Test
    void handlesExtraWhitespaceAndNewlines() {
        assertThat(calculator.calculate("one   two\nthree\tfour")).isEqualTo(1);
    }

    private String words(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> "word")
                .collect(Collectors.joining(" "));
    }
}
