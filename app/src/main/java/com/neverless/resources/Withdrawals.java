package com.neverless.resources;

import com.neverless.domain.account.ExternalAddress;
import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.transaction.TransactionId;
import com.neverless.service.WithdrawalHandler;
import com.neverless.service.WithdrawalStateChecker;
import io.javalin.http.Context;

import java.math.BigDecimal;

public class Withdrawals {
    private final WithdrawalStateChecker withdrawalStateChecker;
    private final WithdrawalHandler withdrawalHandler;

    public Withdrawals(WithdrawalStateChecker withdrawalStateChecker,
                       WithdrawalHandler withdrawalHandler) {
        this.withdrawalStateChecker = withdrawalStateChecker;
        this.withdrawalHandler = withdrawalHandler;
    }

    public void withdrawMoney(Context context) {
        final var body = context.bodyAsClass(WithdrawalRequest.class);

        final var result = withdrawalHandler.withdraw(
            new Money(body.amount),
            body.fromAccount,
            body.toAddress);
        context.status(201);
        context.json(new WithdrawalResponse(result.value().toString()));
    }

    public void getState(Context context) {
        final var id = parseTransactionId(context);
        final var state = withdrawalStateChecker.checkWithdrawState(id);

        if (state.isEmpty()) {
            context.status(404);
            return;
        }

        context.json(new WithdrawalStateResponse(state.get().toString()));
        context.status(200);
    }

    private TransactionId parseTransactionId(Context context) {
        return TransactionId.fromString(context.pathParam("id"));
    }

    private record WithdrawalRequest(BigDecimal amount,
                                     AccountId fromAccount,
                                     ExternalAddress toAddress) {

    }

    private record WithdrawalResponse(String id) {

    }

    private record WithdrawalStateResponse(String state) {

    }
}
