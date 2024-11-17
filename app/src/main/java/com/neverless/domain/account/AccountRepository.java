package com.neverless.domain.account;

import com.neverless.domain.ExternalAddress;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> find(AccountId id);
    Account get(AccountId id);
    Account update(Account account);
    Optional<ExternalAccount> find(ExternalAddress externalAddress);
    Account add(Account account);
}
