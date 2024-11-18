package com.neverless.domain.account;

import com.neverless.domain.Money;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class ExternalAccount extends Account<ExternalAccount> {
    public final ExternalAddress externalAddress;

    protected ExternalAccount(Builder builder) {
        super(builder);
        externalAddress = requireNonNull(builder.externalAddress, "External address can't be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        if (!super.equals(obj)) {
            return false;
        }

        ExternalAccount that = (ExternalAccount) obj;
        return Objects.equals(externalAddress, that.externalAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), externalAddress);
    }

    @Override
    public ExternalAccount.Builder copy() {
        return Builder.externalAccount()
            .baseCopy(this)
            .externalAddress(externalAddress);
    }

    public static class Builder extends Account.Builder<ExternalAccount, ExternalAccount.Builder> {
        private ExternalAddress externalAddress;

        public static ExternalAccount.Builder externalAccount() {
            return new ExternalAccount.Builder()
                .type(AccountType.EXTERNAL)
                .balance(Money.zero())
                .id(AccountId.random());
        }

        public ExternalAccount.Builder externalAddress(ExternalAddress externalAddress) {
            this.externalAddress = externalAddress;
            return this;
        }

        public ExternalAccount build() {
            return new ExternalAccount(this);
        }
    }
}
