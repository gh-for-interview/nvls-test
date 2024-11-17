package com.neverless.service;

import com.neverless.domain.*;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.transaction.*;
import com.neverless.exceptions.NotFoundException;
import com.neverless.integration.WithdrawalService;

import java.util.Optional;
import java.util.UUID;


public class WithdrawalHandler {
    private final WithdrawalService<Money> withdrawalService;
    private final AccountRepository accountRepository;
    private final TransactionManager transactionManager;

    public WithdrawalHandler(WithdrawalService<Money> withdrawalService,
                             AccountRepository accountRepository,
                             TransactionManager transactionManager) {
        this.withdrawalService = withdrawalService;
        this.accountRepository = accountRepository;
        this.transactionManager = transactionManager;
    }

    public TransactionId withdraw(Money amount,
                                  AccountId fromAccountId,
                                  ExternalAddress toAddress) {
        final var maybeExternalAccount = accountRepository.find(toAddress);
        if (maybeExternalAccount.isEmpty()) {
            throw new NotFoundException("Couldn't find account with external address %s".formatted(toAddress));
        }
        final var externalAccount = maybeExternalAccount.get();
        final var id = UUID.randomUUID();
        final var externalRef = new ExternalRef(id.toString());
        final var withdrawalTransaction = transactionManager.transferMoney(
            fromAccountId,
            externalAccount.id,
            amount,
            Optional.of(externalRef));

        try {
            withdrawalService.requestWithdrawal(
                new WithdrawalService.WithdrawalId(id),
                new WithdrawalService.Address(toAddress.value()),
                amount);
        } catch (IllegalArgumentException e) {
            transactionManager.failTransaction(withdrawalTransaction);
            throw e;
        }

        return withdrawalTransaction;
    }

}
