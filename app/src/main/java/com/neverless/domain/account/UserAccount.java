package com.neverless.domain.account;

import com.neverless.domain.Money;

public class UserAccount extends Account<UserAccount> {

    public UserAccount(Account.Builder builder) {
        super(builder);
    }

    public UserAccount.Builder copy() {
        return Builder.userAccount()
            .baseCopy(this);
    }

    public static class Builder extends Account.Builder<UserAccount, UserAccount.Builder> {
        public static Builder userAccount() {
            return new Builder()
                .type(AccountType.INTERNAL)
                .balance(Money.zero())
                .id(AccountId.random());
        }

        public UserAccount build() {
            return new UserAccount(this);
        }
    }
}