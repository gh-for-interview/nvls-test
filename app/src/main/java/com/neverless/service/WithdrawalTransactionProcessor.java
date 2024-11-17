package com.neverless.service;

import com.neverless.domain.transaction.TransactionRepository;

import static com.neverless.domain.transaction.TransactionState.PENDING;
import static com.neverless.domain.transaction.TransactionType.EXTERNAL;

public class WithdrawalTransactionProcessor implements TransactionProcessor {
    private final WithdrawalStateChecker withdrawalStateChecker;
    private final TransactionRepository transactionRepository;
    private final TransactionManager transactionManager;

    public WithdrawalTransactionProcessor(WithdrawalStateChecker withdrawalStateChecker,
                                          TransactionRepository transactionRepository,
                                          TransactionManager transactionManager) {
        this.withdrawalStateChecker = withdrawalStateChecker;
        this.transactionRepository = transactionRepository;
        this.transactionManager = transactionManager;
    }

    public void process() {
        transactionRepository.find(EXTERNAL, PENDING)
            .forEach(transaction -> {
                try {
                    withdrawalStateChecker.checkWithdrawState(transaction.id()).ifPresentOrElse(state -> {
                        switch (state) {
                            case COMPLETED -> transactionManager.completeTransaction(transaction.id());
                            case FAILED -> transactionManager.failTransaction(transaction.id());
                            case PROCESSING -> { }
                        }
                    },
                        () -> transactionManager.failTransaction(transaction.id())
                    );
                } catch (Exception e) {
                    // log
                }
            });
    }
}
