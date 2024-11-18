package com.neverless.service;

import com.neverless.domain.transaction.TransactionRepository;

import static com.neverless.domain.transaction.TransactionState.PENDING;
import static com.neverless.domain.transaction.TransactionType.EXTERNAL;

public class WithdrawalFinalizeJob implements Job {
    private final WithdrawalStateChecker withdrawalStateChecker;
    private final TransactionRepository transactionRepository;
    private final TransactionFinalizer transactionFinalizer;

    public WithdrawalFinalizeJob(WithdrawalStateChecker withdrawalStateChecker,
                                 TransactionRepository transactionRepository,
                                 TransactionFinalizer transactionFinalizer) {
        this.withdrawalStateChecker = withdrawalStateChecker;
        this.transactionRepository = transactionRepository;
        this.transactionFinalizer = transactionFinalizer;
    }

    public void run() {
        transactionRepository.find(EXTERNAL, PENDING)
            .forEach(transaction -> {
                try {
                    withdrawalStateChecker.checkWithdrawState(transaction.id()).ifPresentOrElse(state -> {
                        switch (state) {
                            case COMPLETED -> transactionFinalizer.complete(transaction.id());
                            case FAILED -> transactionFinalizer.fail(transaction.id());
                            case PROCESSING -> { }
                        }
                    },
                        () -> transactionFinalizer.fail(transaction.id())
                    );
                } catch (Exception e) {
                    // log
                }
            });
    }
}
