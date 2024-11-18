package com.neverless.domain.transaction;

import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;

import java.util.Optional;

import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static java.util.Objects.requireNonNull;

public record Transaction(TransactionId id,
                          AccountId from,
                          AccountId to,
                          Money amount,
                          TransactionState state,
                          TransactionType type,
                          Optional<ExternalRef> externalRef) {
    public Transaction {
        requireNonNull(id, "id can't be null");
        requireNonNull(from, "from can't be null");
        requireNonNull(to, "to can't be null");
        requireNonNull(amount, "amount can't be null");
        requireNonNull(state, "state can't be null");
        requireNonNull(type, "type can't be null");

        if (from.equals(to)) {
            throw new IllegalArgumentException("Transaction accounts must be different");
        }

        if (amount.value().signum() < 1) {
            throw new IllegalArgumentException("Transaction value must be greater than zero");
        }
    }

    private Transaction(Builder builder) {
        this(
            builder.id,
            builder.from,
            builder.to,
            builder.amount,
            builder.state,
            builder.type,
            builder.externalRef);
    }

    private Transaction.Builder copy() {
        return transaction()
            .id(id)
            .from(from)
            .to(to)
            .state(state)
            .amount(amount)
            .externalRef(externalRef)
            .type(type);
    }

    public Transaction complete() {
        if (state != TransactionState.PENDING) {
            throw new IllegalStateException("Trying to complete transaction %s which is not in PENDING state, but in %s state".formatted(id, state));
        }
        return copy()
            .state(TransactionState.COMPLETED)
            .build();
    }

    public Transaction fail() {
        if (state != TransactionState.PENDING) {
            throw new IllegalStateException("Trying to fail transaction %s which is not in PENDING state, but in %s state".formatted(id, state));
        }
        return copy()
            .state(TransactionState.FAILED)
            .build();
    }

    public static class Builder {
        private TransactionId id;
        private AccountId from;
        private AccountId to;
        private Money amount;
        private TransactionState state;
        private TransactionType type;

        private Optional<ExternalRef> externalRef = Optional.empty();

        public static Builder transaction() {
            return new Builder()
                .state(TransactionState.PENDING)
                .randomId();
        }

        public Builder randomId() {
            this.id = TransactionId.random();
            return this;
        }

        public Builder id(TransactionId id) {
            this.id = id;
            return this;
        }

        public Builder from(AccountId from) {
            this.from = from;
            return this;
        }

        public Builder to(AccountId to) {
            this.to = to;
            return this;
        }

        public Builder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder state(TransactionState state) {
            this.state = state;
            return this;
        }

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder externalRef(Optional<ExternalRef> externalRef) {
            this.externalRef = externalRef;
            return this;
        }

        public Builder externalRef(ExternalRef externalRef) {
            return externalRef(Optional.of(externalRef));
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}
