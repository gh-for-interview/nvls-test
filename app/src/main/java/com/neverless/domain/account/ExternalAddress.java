package com.neverless.domain.account;

import com.fasterxml.jackson.annotation.JsonCreator;

import static java.util.Objects.requireNonNull;

public record ExternalAddress(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ExternalAddress {
        requireNonNull(value, "ExternalAddress must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ExternalAddress must not be blank");
        }
    }
}
