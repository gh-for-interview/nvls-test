package com.neverless.spec.stubs;

import com.neverless.integration.WithdrawalService;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.neverless.integration.WithdrawalService.WithdrawalState.*;


public class WithdrawalServiceStub<T> implements WithdrawalService<T> {
    private final ConcurrentMap<WithdrawalId, Withdrawal<T>> requests = new ConcurrentHashMap<>();

    @Override
    public void requestWithdrawal(WithdrawalId id, Address address, T amount) { // Please substitute T with preferred type
        final var existing = requests.putIfAbsent(id, new Withdrawal<>(PROCESSING, address, amount));
        if (existing != null && !(Objects.equals(existing.address, address) && Objects.equals(existing.amount, amount)))
            throw new IllegalStateException("Withdrawal request with id[%s] is already present".formatted(id));
    }

    public void fail(WithdrawalId id) {
        requests.compute(id, (_, existingRequest) -> new Withdrawal<>(FAILED, existingRequest.address, existingRequest.amount));
    }

    public void complete(WithdrawalId id) {
        requests.compute(id, (_, existingRequest) -> new Withdrawal<>(COMPLETED, existingRequest.address, existingRequest.amount));
    }

    @Override
    public WithdrawalState getRequestState(WithdrawalId id) {
        final var request = requests.get(id);
        if (request == null)
            throw new IllegalArgumentException("Request %s is not found".formatted(id));
        return request.state();
    }

    record Withdrawal<T>(WithdrawalState state, Address address, T amount) {

    }
}

