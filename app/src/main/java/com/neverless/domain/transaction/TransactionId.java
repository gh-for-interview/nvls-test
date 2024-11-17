package com.neverless.domain.transaction;

import java.util.UUID;

public record TransactionId(UUID value) {

    public static TransactionId random() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId fromString(String input) {
        return new TransactionId(UUID.fromString(input));
    }
}
