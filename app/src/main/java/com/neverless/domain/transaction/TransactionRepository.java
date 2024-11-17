package com.neverless.domain.transaction;

import java.util.Collection;
import java.util.Optional;

public interface TransactionRepository {
    Optional<Transaction> find(TransactionId id);
    Transaction get(TransactionId id);
    Collection<Transaction> find(TransactionType type, TransactionState... states);
    Transaction add(Transaction transaction);
    Transaction update(Transaction transaction);
}
