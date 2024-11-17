package com.neverless.domain;

import org.junit.jupiter.api.Test;

import static com.neverless.domain.Version.firstVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionTest {

    @Test
    void should_increment_version() {
        // then
        assertThat(firstVersion().increment()).isEqualTo(new Version(2));
    }

    @Test
    void should_throw_when_value_is_zero() {
        // then
        assertThatThrownBy(() -> new Version(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_when_value_is_negative() {
        // then
        assertThatThrownBy(() -> new Version(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}