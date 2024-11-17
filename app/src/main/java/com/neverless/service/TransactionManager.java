package com.neverless.service;

import com.google.common.util.concurrent.Striped;
import com.neverless.domain.Money;
import com.neverless.domain.account.Account;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.account.AccountType;
import com.neverless.domain.transaction.*;
import com.neverless.exceptions.InsufficientBalanceException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static java.util.Optional.empty;

public class TransactionManager {
    private final Striped<Lock> locks;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionManager(TransactionRepository transactionRepository, AccountRepository accountRepository, int lockSize) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.locks = Striped.lock(lockSize);
    }

    public TransactionManager(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this(transactionRepository, accountRepository, 100);
    }

    public TransactionId transferMoney(AccountId from, AccountId to, Money amount) {
        return transferMoney(from, to, amount, empty());
    }

    public TransactionId transferMoney(AccountId from,
                                       AccountId to,
                                       Money amount,
                                       Optional<ExternalRef> externalRef) {
        if (amount.value().signum() != 1) {
            throw new IllegalArgumentException("Transfer amount should be greater than zero");
        }
        return withLockByAccounts(from, to, () -> {
            final var fromTo = retryingConcurrentModifications(() -> {
                final var fromAccount = accountRepository.get(from);
                final var toAccount = accountRepository.get(to);

                if (fromAccount.type.equals(AccountType.EXTERNAL) && toAccount.type.equals(AccountType.EXTERNAL)) {
                    throw new IllegalArgumentException("Transfer between external accounts %s -> %s is not allowed".formatted(from.value(), to.value()));
                }

                if (fromAccount.balance.value().compareTo(amount.value()) < 0) {
                    throw new InsufficientBalanceException("Account %s doesn't have enough balance".formatted(from.value()));
                }

                accountRepository.update(fromAccount.deduct(amount));
                return Pair.of(fromAccount, toAccount);
            });

            final var transaction = transaction()
                .from(from)
                .to(to)
                .amount(amount)
                .type(determineType(fromTo.getLeft(), fromTo.getRight()))
                .externalRef(externalRef)
                .build();
            transactionRepository.add(transaction);
            return transaction.id();
        });
    }

    public void completeTransaction(TransactionId id) {
        withLockByTransaction(id, () -> retryingConcurrentModifications(() -> {
            final var transaction = transactionRepository.get(id);
            return withLockByAccount(transaction.to(), () -> {
                final var toAccount = accountRepository.get(transaction.to());
                accountRepository.update(toAccount.add(transaction.amount()));

                transactionRepository.update(transaction.complete());

                return null;
            });
        }));
    }

    public void failTransaction(TransactionId id) {
        withLockByTransaction(id, () -> retryingConcurrentModifications(() -> {
            final var transaction = transactionRepository.get(id);
            return withLockByAccount(transaction.from(), () -> {
                final var fromAccount = accountRepository.get(transaction.from());
                accountRepository.update(fromAccount.add(transaction.amount()));

                transactionRepository.update(transaction.fail());

                return null;
            });
        }));
    }

    private TransactionType determineType(Account<?> from, Account<?> to) {
        return from.type.equals(AccountType.EXTERNAL) || to.type.equals(AccountType.EXTERNAL) ?
            TransactionType.EXTERNAL :
            TransactionType.INTERNAL;
    }

    private <T> T withLockByAccounts(AccountId from, AccountId to, Supplier<T> executable) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("Can't process transaction when accounts are the same. Account id %s".formatted(from));
        }
        final var firstToLock = from.value().compareTo(to.value()) > 0 ? from : to;
        final var secondToLock = from.value().compareTo(to.value()) > 0 ? to : from;

        return withLockByAccount(firstToLock, () -> withLockByAccount(secondToLock, executable));
    }

    private <T> T withLockByAccount(AccountId id, Supplier<T> executable) {
        final var tranactionLock = locks.get(id.toString());
        tranactionLock.lock();

        try {
            return executable.get();
        } finally {
            tranactionLock.unlock();
        }
    }

    private <T> T withLockByTransaction(TransactionId id, Supplier<T> executable) {
        final var tranactionLock = locks.get(id.toString());
        tranactionLock.lock();

        try {
            return executable.get();
        } finally {
            tranactionLock.unlock();
        }
    }

    private <T> T retryingConcurrentModifications(Supplier<T> executable) {
        var retries = 0;
        while (retries < 3) {
            try {
                return executable.get();
            } catch (ConcurrentModificationException e) {
                retries++;
            }
        }
        throw new IllegalStateException("Couldn't process data after %s retries".formatted(retries));
    }
}
