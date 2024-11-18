package com.neverless.service;

import com.neverless.domain.Money;
import com.neverless.domain.transaction.WithdrawalTransactionState;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.transaction.*;
import com.neverless.integration.WithdrawalService;
import com.neverless.integration.WithdrawalService.WithdrawalId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.UUID;

import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class WithdrawalStateCheckerTest {
    WithdrawalService<Money> withdrawalService = mock(WithdrawalService.class);
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    WithdrawalStateChecker withdrawalStateChecker = new WithdrawalStateChecker(withdrawalService, transactionRepository);

    @ParameterizedTest
    @CsvSource(value = """
        PROCESSING | PROCESSING
        COMPLETED  | COMPLETED
        FAILED     | FAILED""", delimiter = '|')
    void returns_status(WithdrawalService.WithdrawalState state, WithdrawalTransactionState expected) {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.get(transaction.id())).willReturn(transaction);
        given(withdrawalService.getRequestState(new WithdrawalId(UUID.fromString(transaction.externalRef().get().value())))).willReturn(state);

        // when
        var actual = withdrawalStateChecker.checkWithdrawState(transaction.id());

        // then
        assertThat(actual).contains(expected);
    }

    @Test
    void returns_empty_when_id_not_found() {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.get(transaction.id())).willReturn(transaction);
        given(withdrawalService.getRequestState(new WithdrawalId(UUID.fromString(transaction.externalRef().get().value()))))
            .willThrow(new IllegalArgumentException("Unable to find id"));

        // when
        var actual = withdrawalStateChecker.checkWithdrawState(transaction.id());

        // then
        assertThat(actual).isEmpty();
    }

    @Test
    void rethrows_when_withdrawal_service_throws() {
        // given
        var transaction = aTransaction().build();
        given(transactionRepository.get(transaction.id())).willReturn(transaction);
        given(withdrawalService.getRequestState(new WithdrawalId(UUID.fromString(transaction.externalRef().get().value()))))
                .willThrow(new RuntimeException("test exception"));

        // then
        assertThatThrownBy(() -> withdrawalStateChecker.checkWithdrawState(transaction.id()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("test exception");
    }

    private Transaction.Builder aTransaction() {
        return transaction()
            .amount(new Money(BigDecimal.TEN))
            .from(AccountId.random())
            .to(AccountId.random())
            .externalRef(new ExternalRef(UUID.randomUUID().toString()))
            .type(TransactionType.EXTERNAL);
    }
}