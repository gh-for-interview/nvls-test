package com.neverless.service;

import com.neverless.domain.Money;
import com.neverless.domain.WithdrawalTransactionState;
import com.neverless.domain.transaction.TransactionId;
import com.neverless.domain.transaction.TransactionRepository;
import com.neverless.exceptions.NotFoundException;
import com.neverless.integration.WithdrawalService;
import com.neverless.integration.WithdrawalService.WithdrawalId;

import java.util.Optional;
import java.util.UUID;

public class WithdrawalStateChecker {
    private final WithdrawalService<Money> withdrawalService;
    private final TransactionRepository transactionRepository;

    public WithdrawalStateChecker(WithdrawalService<Money> withdrawalService,
                                  TransactionRepository transactionRepository) {
        this.withdrawalService = withdrawalService;
        this.transactionRepository = transactionRepository;
    }

    public Optional<WithdrawalTransactionState> checkWithdrawState(TransactionId id) {
        final var transaction = transactionRepository.get(id);

        if (transaction.externalRef().isEmpty()) {
            return Optional.empty();
        }

        final var withdrawalId = new WithdrawalId(UUID.fromString(transaction.externalRef().get().value()));

        try {
            return Optional.of(switch (withdrawalService.getRequestState(withdrawalId)) {
                case FAILED -> WithdrawalTransactionState.FAILED;
                case COMPLETED -> WithdrawalTransactionState.COMPLETED;
                case PROCESSING -> WithdrawalTransactionState.PROCESSING;
            });
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
