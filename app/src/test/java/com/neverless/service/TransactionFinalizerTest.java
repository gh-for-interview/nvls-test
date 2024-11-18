package com.neverless.service;

import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.transaction.TransactionRepository;
import com.neverless.domain.transaction.TransactionState;
import com.neverless.domain.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class TransactionFinalizerTest {
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    MoneyMover moneyMover = mock(MoneyMover.class);
    LockManager lockManager = mock(LockManager.class);
    TransactionFinalizer transactionFinalizer = new TransactionFinalizer(transactionRepository, moneyMover, lockManager);

    AccountId from = AccountId.random();
    AccountId to = AccountId.random();
    Money amount = new Money(BigDecimal.ONE);

    @BeforeEach
    public void setup() {
        given(lockManager.withLockBy(any(), any())).willAnswer(invocationOnMock -> {
            final var executable = (Supplier<Object>) invocationOnMock.getArgument(1);
            return executable.get();
        });
    }

    @Test
    void should_complete_transaction_and_move_funds() {
        // given
        final var transaction = transaction()
            .amount(amount)
            .state(TransactionState.PENDING)
            .type(TransactionType.INTERNAL)
            .from(from)
            .to(to)
            .build();
        given(transactionRepository.get(transaction.id())).willReturn(transaction);

        // when
        transactionFinalizer.complete(transaction.id());

        // then
        then(transactionRepository).should(times(1)).update(transaction.complete());
        then(moneyMover).should(times(1)).addFunds(to, amount);
    }

    @Test
    void should_fail_transaction_and_return_funds() {
        // given
        final var transaction = transaction()
            .amount(amount)
            .state(TransactionState.PENDING)
            .type(TransactionType.INTERNAL)
            .from(from)
            .to(to)
            .build();
        given(transactionRepository.get(transaction.id())).willReturn(transaction);

        // when
        transactionFinalizer.fail(transaction.id());

        // then
        then(transactionRepository).should(times(1)).update(transaction.fail());
        then(moneyMover).should(times(1)).addFunds(from, amount);
    }
}