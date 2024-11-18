package com.neverless.storage;

import com.neverless.domain.transaction.*;
import com.neverless.exceptions.NotFoundException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TransactionRepositoryInMem implements TransactionRepository {
    private final Map<TransactionId, Transaction> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<Transaction> find(TransactionId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Transaction get(TransactionId id) {
        final var result = storage.get(id);
        if (result == null) {
            throw new NotFoundException("Transaction %s does not exist.".formatted(id.value()));
        }
        return storage.get(id);
    }

    @Override
    public Collection<Transaction> find(TransactionType type, TransactionState... states) {
        final var stateSet = Arrays.stream(states).collect(Collectors.toSet());
        return storage
            .values()
            .stream()
            .filter(transaction -> transaction.type().equals(type))
            .filter(transaction -> stateSet.contains(transaction.state()))
            .toList();
    }

    @Override
    public Transaction add(Transaction transaction) {
        if (storage.containsKey(transaction.id())) {
            throw new IllegalStateException("Transaction %s already exists.".formatted(transaction.id().value()));
        }
        storage.put(transaction.id(), transaction);
        return transaction;
    }

    @Override
    public Transaction update(Transaction transaction) {
        return storage.compute(transaction.id(), (_, currentValue) -> {
            if (currentValue == null) {
                throw new NotFoundException("Transaction %s does not exists.".formatted(transaction.id().value()));
            }

            return transaction;
        });
    }
}
