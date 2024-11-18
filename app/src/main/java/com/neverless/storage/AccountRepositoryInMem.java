package com.neverless.storage;

import com.google.common.util.concurrent.Striped;
import com.neverless.domain.account.ExternalAddress;
import com.neverless.domain.account.Account;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.account.ExternalAccount;
import com.neverless.exceptions.NotFoundException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class AccountRepositoryInMem implements AccountRepository {
    private final Striped<Lock> locks = Striped.lock(100);
    private final Map<AccountId, Account> storage = new ConcurrentHashMap<>();
    private final Map<ExternalAddress, ExternalAccount> storageByExternalAddress = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> find(AccountId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Account get(AccountId id) {
        final var result = storage.get(id);
        if (result == null) {
            throw new NotFoundException("Account %s does not exists.".formatted(id));
        }
        return result;
    }

    @Override
    public Account update(Account account) {
        return withLockByAccount(account.id, () -> storage.compute(account.id, (_, currentValue) -> {
            if (currentValue == null) {
                throw new NotFoundException("Account %s does not exists.".formatted(account.id.value()));
            }

            if (account instanceof ExternalAccount externalAccount) {
                if (!storageByExternalAddress.containsKey(externalAccount.externalAddress)) {
                    throw new IllegalStateException("Mismatch between account id %s and external address %s"
                            .formatted(account.id.value(), externalAccount.externalAddress.value()));
                }
                storageByExternalAddress.put(externalAccount.externalAddress, externalAccount);
            }
            return account;
        }));
    }

    @Override
    public Optional<ExternalAccount> find(ExternalAddress externalAddress) {
        return Optional.ofNullable(storageByExternalAddress.get(externalAddress));
    }

    @Override
    public Account add(Account account) {
        return withLockByAccount(account.id, () -> {
            if (storage.containsKey(account.id)) {
                throw new IllegalStateException("Attempting to add account %s which is already present".formatted(account.id.value()));
            }

            if (account instanceof ExternalAccount externalAccount) {
                if (storageByExternalAddress.containsKey(externalAccount.externalAddress)) {
                    throw new IllegalStateException("Attempting to add account %s which is already present".formatted(account.id.value()));
                }

                storageByExternalAddress.put(externalAccount.externalAddress, externalAccount);
            }

            storage.put(account.id, account);

            return account;
        });
    }

    private <T> T withLockByAccount(AccountId id, Supplier<T> executable) {
        final var lock = locks.get(id.value());
        lock.lock();

        try {
            return executable.get();
        } finally {
            lock.unlock();
        }
    }
}
