package com.neverless.service;

import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.transaction.Transaction;
import com.neverless.domain.transaction.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.neverless.domain.transaction.WithdrawalTransactionState.COMPLETED;
import static com.neverless.domain.transaction.WithdrawalTransactionState.FAILED;
import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static com.neverless.domain.transaction.TransactionState.PENDING;
import static com.neverless.domain.transaction.TransactionType.EXTERNAL;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class WithdrawalFinalizeJobTest {
    WithdrawalStateChecker withdrawalStateChecker = mock(WithdrawalStateChecker.class);
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    TransactionFinalizer transactionFinalizer = mock(TransactionFinalizer.class);
    WithdrawalFinalizeJob processor = new WithdrawalFinalizeJob(
        withdrawalStateChecker,
        transactionRepository,
        transactionFinalizer);

    @Test
    void does_nothing_when_there_are_no_pending_transactions() {
        // given
        given(transactionRepository.find(EXTERNAL, PENDING)).willReturn(Collections.emptyList());

        // when
        processor.run();

        // then
        then(withdrawalStateChecker).shouldHaveNoInteractions();
        then(transactionFinalizer).shouldHaveNoInteractions();
    }

    @Test
    void completes_transaction_when_its_completed_in_source() {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.find(EXTERNAL, PENDING)).willReturn(List.of(transaction));
        given(withdrawalStateChecker.checkWithdrawState(transaction.id())).willReturn(Optional.of(COMPLETED));

        // when
        processor.run();

        // then
        then(transactionFinalizer).should(times(1)).complete(transaction.id());
    }

    @Test
    void fails_transaction_when_its_failed_in_source() {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.find(EXTERNAL, PENDING)).willReturn(List.of(transaction));
        given(withdrawalStateChecker.checkWithdrawState(transaction.id())).willReturn(Optional.of(FAILED));

        // when
        processor.run();

        // then
        then(transactionFinalizer).should(times(1)).fail(transaction.id());
    }

    @Test
    void fails_transaction_when_its_not_found_in_source() {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.find(EXTERNAL, PENDING)).willReturn(List.of(transaction));
        given(withdrawalStateChecker.checkWithdrawState(transaction.id())).willReturn(Optional.empty());

        // when
        processor.run();

        // then
        then(transactionFinalizer).should(times(1)).fail(transaction.id());
    }

    private Transaction.Builder aTransaction() {
        return transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(new Money(BigDecimal.TEN))
            .type(EXTERNAL);
    }
}