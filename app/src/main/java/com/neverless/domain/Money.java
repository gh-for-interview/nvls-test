package com.neverless.domain;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

public record Money(BigDecimal value) {
    public Money {
        requireNonNull(value, "Value can't be zero");
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money subtract(Money amount) {
        return new Money(value.subtract(amount.value));
    }

    public Money add(Money amount) {
        return new Money(value.add(amount.value));
    }
}
