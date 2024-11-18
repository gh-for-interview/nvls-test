package com.neverless.service;

import com.neverless.domain.transaction.TransactionId;
import com.neverless.domain.transaction.TransactionRepository;


public class TransactionFinalizer {
    private final TransactionRepository transactionRepository;
    private final MoneyMover moneyMover;
    private final LockManager lockManager;

    public TransactionFinalizer(TransactionRepository transactionRepository,
                                MoneyMover moneyMover,
                                LockManager lockManager) {
        this.transactionRepository = transactionRepository;
        this.moneyMover = moneyMover;
        this.lockManager = lockManager;
    }

    public void complete(TransactionId id) {
        lockManager.withLockBy(id.value().toString(), () -> {
            final var transaction = transactionRepository.get(id);
            return lockManager.withLockBy(transaction.to().value().toString(), () -> {
                moneyMover.addFunds(transaction.to(), transaction.amount());

                transactionRepository.update(transaction.complete());

                return null;
            });
        });
    }

    public void fail(TransactionId id) {
        lockManager.withLockBy(id.value().toString(), () -> {
            final var transaction = transactionRepository.get(id);
            return lockManager.withLockBy(transaction.from().value().toString(), () -> {
                moneyMover.addFunds(transaction.from(), transaction.amount());

                transactionRepository.update(transaction.fail());

                return null;
            });
        });
    }
}
