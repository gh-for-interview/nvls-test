package com.neverless.domain;

import com.neverless.domain.account.ExternalAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalAddressTest {

    @Test
    void should_throw_exception_when_value_is_null() {
        // then
        assertThatThrownBy(() -> new ExternalAddress(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_exception_when_value_is_blank() {
        // then
        assertThatThrownBy(() -> new ExternalAddress("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}