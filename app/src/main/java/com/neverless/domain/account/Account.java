package com.neverless.domain.account;

import com.neverless.domain.Money;
import com.neverless.domain.Version;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public abstract class Account<T extends Account<T>> {

    public final AccountId id;
    public final AccountType type;
    public final Money balance;
    public final Version version;

    protected Account(Builder builder) {
        this.id = requireNonNull(builder.id, "Id must not be null");
        this.type = requireNonNull(builder.type, "Type must not be null");
        this.balance = requireNonNull(builder.balance, "Balance must not be null");
        this.version = requireNonNull(builder.version, "Version must not be null");
    }

    public T deduct(Money amount) {
        return copy()
            .balance(balance.subtract(amount))
            .version(version.increment())
            .build();
    }

    public T add(Money amount) {
        return copy()
            .balance(balance.add(amount))
            .version(version.increment())
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
                Objects.equals(balance, that.balance) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, type, balance, version);
    }

    protected abstract <B extends Builder<T, B>> Builder<T, B> copy();

    public abstract static class Builder<T extends Account<T>, B extends Builder<T, B>> {
        private AccountId id;
        private AccountType type;
        private Money balance;
        private Version version = Version.firstVersion();

        public B baseCopy(T account) {
            id(account.id)
                .type(account.type)
                .balance(account.balance)
                .version(account.version);

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

        public B version(Version version) {
            this.version = version;
            return (B) this;
        }

        public abstract T build();
    }
}
