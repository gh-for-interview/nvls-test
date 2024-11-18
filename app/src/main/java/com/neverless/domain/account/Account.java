package com.neverless.domain.account;

import com.neverless.domain.Money;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public abstract class Account<T extends Account<T>> {

    public final AccountId id;
    public final AccountType type;
    public final Money balance;

    protected Account(Builder builder) {
        this.id = requireNonNull(builder.id, "Id must not be null");
        this.type = requireNonNull(builder.type, "Type must not be null");
        this.balance = requireNonNull(builder.balance, "Balance must not be null");
    }

    public T deduct(Money amount) {
        return copy()
            .balance(balance.subtract(amount))
            .build();
    }

    public T add(Money amount) {
        return copy()
            .balance(balance.add(amount))
            .build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Account that = (Account) obj;
        return Objects.equals(id, that.id) &&
                Objects.equals(type, that.type) &&
                Objects.equals(balance, that.balance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, type, balance);
    }

    protected abstract <B extends Builder<T, B>> Builder<T, B> copy();

    public abstract static class Builder<T extends Account<T>, B extends Builder<T, B>> {
        private AccountId id;
        private AccountType type;
        private Money balance;

        public B baseCopy(T account) {
            id(account.id)
                .type(account.type)
                .balance(account.balance);

            return (B) this;
        }

        public B id(AccountId id) {
            this.id = id;
            return (B) this;
        }

        public B type(AccountType type) {
            this.type = type;
            return (B) this;
        }

        public B balance(Money balance) {
            this.balance = balance;
            return (B) this;
        }

        public abstract T build();
    }
}
