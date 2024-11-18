package com.neverless.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.neverless.domain.account.AccountRepository;
import com.neverless.exceptions.InsufficientBalanceException;
import com.neverless.exceptions.NotFoundException;
import com.neverless.service.WithdrawalHandler;
import com.neverless.service.WithdrawalStateChecker;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRouting;

import java.util.UUID;

public class Resources {
    private final Healthcheck healthcheck;
    private final Accounts accounts;
    private final Withdrawals withdrawals;

    public Resources(AccountRepository accountRepo,
                     WithdrawalStateChecker withdrawalStateChecker,
                     WithdrawalHandler withdrawalHandler) {
        healthcheck = new Healthcheck();
        accounts = new Accounts(accountRepo);
        withdrawals = new Withdrawals(withdrawalStateChecker, withdrawalHandler);
    }

    public void register(JavalinDefaultRouting router) {
        router.exception(NotFoundException.class, (ex, ctx) -> handleError(404, ex, ctx));
        router.exception(InsufficientBalanceException.class, (ex, ctx) -> handleError(400, ex, ctx));
        router.exception(IllegalArgumentException.class, (ex, ctx) -> handleError(400, ex, ctx));
        router.exception(JsonParseException.class, (ex, ctx) -> handleError(400, ex, ctx));

        router.get("/accounts/{id}", accounts::get);
        router.post("/withdrawal", withdrawals::withdrawMoney);
        router.get("/withdrawal/{id}/state", withdrawals::getState);

        router.get("/healthcheck", healthcheck::check);
    }

    private void handleError(int status, Exception e, Context context) {
        // log e
        e.printStackTrace();
        final var errorId = UUID.randomUUID();
        context.json(new ErrorResponse("Something went wrong processing your request, error id %s".formatted(errorId)));
        context.status(status);
    }
}
