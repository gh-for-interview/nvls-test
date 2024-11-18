package com.neverless.service;

import com.neverless.domain.Money;
import com.neverless.domain.account.Account;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.account.AccountType;
import com.neverless.domain.transaction.*;
import com.neverless.exceptions.InsufficientBalanceException;

import java.util.Optional;
import java.util.function.Supplier;

import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;

public class MoneyMover {
    private final LockManager lockManager;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public MoneyMover(TransactionRepository transactionRepository,
                      AccountRepository accountRepository,
                      LockManager lockManager) {
        this.transactionRepository = requireNonNull(transactionRepository);
        this.accountRepository = requireNonNull(accountRepository);
        this.lockManager = requireNonNull(lockManager);
    }

    public TransactionId moveMoney(AccountId from, AccountId to, Money amount) {
        return moveMoney(from, to, amount, empty());
    }

    public TransactionId moveMoney(AccountId from,
                                   AccountId to,
                                   Money amount,
                                   Optional<ExternalRef> externalRef) {
        if (amount.value().signum() != 1) {
            throw new IllegalArgumentException("Transfer amount should be greater than zero");
        }
        return withLockByAccounts(from, to, () -> {
            final var fromAccount = accountRepository.get(from);
            final var toAccount = accountRepository.get(to);

            if (fromAccount.type.equals(AccountType.EXTERNAL) && toAccount.type.equals(AccountType.EXTERNAL)) {
                throw new IllegalArgumentException("Transfer between external accounts %s -> %s is not allowed".formatted(from.value(), to.value()));
            }

            if (fromAccount.balance.value().compareTo(amount.value()) < 0) {
                throw new InsufficientBalanceException("Account %s doesn't have enough balance".formatted(from.value()));
            }

            accountRepository.update(fromAccount.deduct(amount));

            final var transaction = transaction()
                .from(from)
                .to(to)
                .amount(amount)
                .type(determineType(fromAccount, toAccount))
                .externalRef(externalRef)
                .build();
            transactionRepository.add(transaction);
            return transaction.id();
        });
    }

    public void addMoney(AccountId id, Money amount) {
        if (amount.value().signum() < 1) {
            throw new IllegalArgumentException("Amount should be greater than zero");
        }
        lockManager.withLockBy(id.value().toString(), () -> {
            final var acc = accountRepository.get(id);
            accountRepository.update(acc.add(amount));

            return null;
        });
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

        return lockManager.withLockBy(firstToLock.value().toString(), () -> lockManager.withLockBy(secondToLock.value().toString(), executable));
    }
}
