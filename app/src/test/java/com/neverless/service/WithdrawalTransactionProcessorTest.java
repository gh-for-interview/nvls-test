package com.neverless.service;

import com.neverless.domain.Money;
import com.neverless.domain.WithdrawalTransactionState;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.transaction.Transaction;
import com.neverless.domain.transaction.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.neverless.domain.WithdrawalTransactionState.COMPLETED;
import static com.neverless.domain.WithdrawalTransactionState.FAILED;
import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static com.neverless.domain.transaction.TransactionState.PENDING;
import static com.neverless.domain.transaction.TransactionType.EXTERNAL;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class WithdrawalTransactionProcessorTest {
    WithdrawalStateChecker withdrawalStateChecker = mock(WithdrawalStateChecker.class);
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    TransactionManager transactionManager = mock(TransactionManager.class);
    WithdrawalTransactionProcessor processor = new WithdrawalTransactionProcessor(
            withdrawalStateChecker,
        transactionRepository,
        transactionManager);

    @Test
    void does_nothing_when_there_are_no_pending_transactions() {
        // given
        given(transactionRepository.find(EXTERNAL, PENDING)).willReturn(Collections.emptyList());

        // when
        processor.process();

        // then
        then(withdrawalStateChecker).shouldHaveNoInteractions();
        then(transactionManager).shouldHaveNoInteractions();
    }

    @Test
    void completes_transaction_when_its_completed_in_source() {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.find(EXTERNAL, PENDING)).willReturn(List.of(transaction));
        given(withdrawalStateChecker.checkWithdrawState(transaction.id())).willReturn(Optional.of(COMPLETED));

        // when
        processor.process();

        // then
        then(transactionManager).should(times(1)).completeTransaction(transaction.id());
    }

    @Test
    void fails_transaction_when_its_failed_in_source() {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.find(EXTERNAL, PENDING)).willReturn(List.of(transaction));
        given(withdrawalStateChecker.checkWithdrawState(transaction.id())).willReturn(Optional.of(FAILED));

        // when
        processor.process();

        // then
        then(transactionManager).should(times(1)).failTransaction(transaction.id());
    }

    @Test
    void fails_transaction_when_its_not_found_in_source() {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.find(EXTERNAL, PENDING)).willReturn(List.of(transaction));
        given(withdrawalStateChecker.checkWithdrawState(transaction.id())).willReturn(Optional.empty());

        // when
        processor.process();

        // then
        then(transactionManager).should(times(1)).failTransaction(transaction.id());
    }

    private Transaction.Builder aTransaction() {
        return transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(new Money(BigDecimal.TEN))
            .type(EXTERNAL);
    }
}