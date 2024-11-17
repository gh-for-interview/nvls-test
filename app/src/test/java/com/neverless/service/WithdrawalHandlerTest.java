package com.neverless.service;

import com.neverless.domain.ExternalAddress;
import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.account.ExternalAccount;
import com.neverless.domain.transaction.TransactionId;
import com.neverless.exceptions.NotFoundException;
import com.neverless.integration.WithdrawalService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static com.neverless.domain.account.ExternalAccount.Builder.externalAccount;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class WithdrawalHandlerTest {
    WithdrawalService<Money> withdrawalService = mock(WithdrawalService.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    TransactionManager transactionManager = mock(TransactionManager.class);
    WithdrawalHandler withdrawalHandler = new WithdrawalHandler(
        withdrawalService,
        accountRepository,
        transactionManager);

    Money amount = new Money(BigDecimal.TEN);
    ExternalAddress externalAddress = new ExternalAddress(randomAlphabetic(8));
    ExternalAccount externalAccount = externalAccount().externalAddress(externalAddress).build();
    AccountId accountId = AccountId.random();

    @Test
    void should_throw_when_external_account_not_found() {
        // given
        given(accountRepository.find(externalAddress)).willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> withdrawalHandler.withdraw(amount, accountId, externalAddress))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_and_revert_transaction_when_withdrawal_service_throws() {
        // given
        given(accountRepository.find(externalAddress)).willReturn(Optional.of(externalAccount));

        var transactionId = TransactionId.random();
        given(transactionManager.transferMoney(eq(accountId), eq(externalAccount.id), eq(amount), any()))
            .willReturn(transactionId);

        doThrow(new IllegalArgumentException("test"))
            .when(withdrawalService)
            .requestWithdrawal(any(WithdrawalService.WithdrawalId.class), eq(new WithdrawalService.Address(externalAddress.value())), eq(amount));

        // then
        assertThatThrownBy(() -> withdrawalHandler.withdraw(amount, accountId, externalAddress))
            .isInstanceOf(IllegalArgumentException.class);
        then(transactionManager).should(times(1)).failTransaction(transactionId);
    }

    @Test
    void should_request_withdrawal() {
        // given
        given(accountRepository.find(externalAddress)).willReturn(Optional.of(externalAccount));

        var transactionId = TransactionId.random();
        given(transactionManager.transferMoney(eq(accountId), eq(externalAccount.id), eq(amount), any()))
            .willReturn(transactionId);

        // when
        var result = withdrawalHandler.withdraw(amount, accountId, externalAddress);

        // then
        then(withdrawalService).should(times(1))
            .requestWithdrawal(any(WithdrawalService.WithdrawalId.class), eq(new WithdrawalService.Address(externalAddress.value())), eq(amount));
        assertThat(result).isEqualTo(transactionId);
    }

}