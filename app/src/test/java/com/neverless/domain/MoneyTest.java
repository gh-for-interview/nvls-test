package com.neverless.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.neverless.domain.Money.zero;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void should_subtract_money() {
        // then
        assertThat(zero().subtract(new Money(BigDecimal.TEN))).isEqualTo(new Money(BigDecimal.TEN.negate()));
    }

    @Test
    void should_add_money() {
        // then
        assertThat(zero().add(new Money(BigDecimal.TEN))).isEqualTo(new Money(BigDecimal.TEN));
    }

    @Test
    void should_throw_when_value_is_null() {
        // then
       assertThatThrownBy(() -> new Money(null)).isInstanceOf(NullPointerException.class);
    }

}