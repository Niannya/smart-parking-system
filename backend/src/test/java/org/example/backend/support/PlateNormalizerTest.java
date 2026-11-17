package org.example.backend.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlateNormalizerTest {

    @Test
    void removesAllWhitespaceAndUppercases() {
        assertThat(PlateNormalizer.normalize(" 京 a 12345 ")).isEqualTo("京A12345");
    }

    @Test
    void nullBecomesEmpty() {
        assertThat(PlateNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void acceptsOrdinarySmallPlatesWithFiveOrSixCharacters() {
        assertThat(PlateNormalizer.isValid("京A12345")).isTrue();
        assertThat(PlateNormalizer.isValid("京A123456")).isTrue();
        assertThat(PlateNormalizer.isValid("川A12345D")).isTrue();
    }

    @Test
    void rejectsWrongFormat() {
        assertThat(PlateNormalizer.isValid("京A1234")).isFalse();
        assertThat(PlateNormalizer.isValid("A12345")).isFalse();
        assertThat(PlateNormalizer.isValid("京A1234O")).isFalse();
        assertThat(PlateNormalizer.isValid("")).isFalse();
    }
}
