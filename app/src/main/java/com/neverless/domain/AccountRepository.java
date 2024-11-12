package com.neverless.domain;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> find(AccountId id);
}
