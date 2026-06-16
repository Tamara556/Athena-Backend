package com.athena.badge.service;

import com.athena.common.event.BadgeSuggestion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeSuggestionValidatorTest {

    @Test
    void acceptsWellFormedSuggestion() {
        assertThat(BadgeSuggestionValidator.isValid(
                new BadgeSuggestion("BUG_SLAYER", "Bug Slayer", "Fixed 10 bugs", "🐛"))).isTrue();
    }

    @Test
    void rejectsLowercaseOrSpacedCode() {
        assertThat(BadgeSuggestionValidator.isValid(
                new BadgeSuggestion("bug slayer", "Bug Slayer", "x", "🐛"))).isFalse();
    }

    @Test
    void rejectsBlankName() {
        assertThat(BadgeSuggestionValidator.isValid(
                new BadgeSuggestion("BUG_SLAYER", "  ", "x", "🐛"))).isFalse();
    }

    @Test
    void rejectsNullSuggestion() {
        assertThat(BadgeSuggestionValidator.isValid(null)).isFalse();
    }
}
