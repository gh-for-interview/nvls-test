package com.neverless.service;

import com.neverless.domain.ExternalAddress;
import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.transaction.ExternalRef;
import com.neverless.domain.transaction.TransactionRepository;
import com.neverless.domain.transaction.TransactionState;
import com.neverless.domain.transaction.TransactionType;
import com.neverless.exceptions.InsufficientBalanceException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.*;

import static com.neverless.domain.account.ExternalAccount.Builder.externalAccount;
import static com.neverless.domain.account.UserAccount.Builder.userAccount;
import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class TransactionManagerTest {
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    TransactionManager transactionManager = new TransactionManager(transactionRepository, accountRepository);

    AccountId from = AccountId.random();
    AccountId to = AccountId.random();
    Money amount = new Money(BigDecimal.ONE);

    @Nested
    class TransferMoneyTest {
        @Test
        void should_throw_when_transfer_between_external_accounts() {
            // given
            given(accountRepository.get(from)).willReturn(externalAccount()
                .externalAddress(new ExternalAddress(randomAlphabetic(8)))
                .balance(new Money(BigDecimal.TEN))
                .build());
            given(accountRepository.get(to)).willReturn(externalAccount()
                .externalAddress(new ExternalAddress(randomAlphabetic(8)))
                .build());

            // then
            assertThatThrownBy(() -> transactionManager.transferMoney(from, to, new Money(BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_amount_is_negative() {
            // then
            assertThatThrownBy(() -> transactionManager.transferMoney(from, to, new Money(BigDecimal.TEN.negate())))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_amount_is_zero() {
            // then
            assertThatThrownBy(() -> transactionManager.transferMoney(from, to, Money.zero()))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_account_is_the_same() {
            // then
            assertThatThrownBy(() -> transactionManager.transferMoney(from, from, amount))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_account_has_insufficient_balance() {
            // given
            given(accountRepository.get(from)).willReturn(userAccount().balance(Money.zero()).build());
            given(accountRepository.get(to)).willReturn(userAccount().build());

            // then
            assertThatThrownBy(() -> transactionManager.transferMoney(from, to, amount))
                .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        void should_create_transaction_and_lock_funds() {
            // given
            final var fromAcc = userAccount().balance(new Money(BigDecimal.TEN)).build();
            given(accountRepository.get(from)).willReturn(fromAcc);
            given(accountRepository.get(to)).willReturn(userAccount().build());

            // when
            final var result = transactionManager.transferMoney(from, to, amount);

            // then
            assertThat(result).isNotNull();
            final var expectedTransaction = transaction()
                .id(result)
                .amount(amount)
                .state(TransactionState.PENDING)
                .type(TransactionType.INTERNAL)
                .from(from)
                .to(to)
                .build();
            then(transactionRepository).should(times(1)).add(expectedTransaction);
            then(accountRepository).should(times(1)).update(fromAcc.deduct(amount));
        }

        @Test
        void should_create_transaction_when_external_ref_is_provided() {
            // given
            final var fromAcc = userAccount().balance(new Money(BigDecimal.TEN)).build();
            given(accountRepository.get(from)).willReturn(fromAcc);
            given(accountRepository.get(to)).willReturn(userAccount().build());

            final var externalRef = new ExternalRef("external-ref");

            // when
            final var result = transactionManager.transferMoney(from, to, amount, Optional.of(externalRef));

            // then
            assertThat(result).isNotNull();
            final var expectedTransaction = transaction()
                .id(result)
                .amount(amount)
                .state(TransactionState.PENDING)
                .type(TransactionType.INTERNAL)
                .from(from)
                .to(to)
                .externalRef(externalRef)
                .build();
            then(transactionRepository).should(times(1)).add(expectedTransaction);
            then(accountRepository).should(times(1)).update(fromAcc.deduct(amount));
        }

        @Test
        void should_create_transaction_with_correct_type_when_transaction_is_external() {
            // given
            final var fromAcc = userAccount().balance(new Money(BigDecimal.TEN)).build();
            given(accountRepository.get(from)).willReturn(fromAcc);
            given(accountRepository.get(to)).willReturn(externalAccount().externalAddress(new ExternalAddress(randomAlphabetic(8))).build());

            // when
            final var result = transactionManager.transferMoney(from, to, amount);

            // then
            assertThat(result).isNotNull();
            final var expectedTransaction = transaction()
                .id(result)
                .amount(amount)
                .state(TransactionState.PENDING)
                .type(TransactionType.EXTERNAL)
                .from(from)
                .to(to)
                .build();
            then(transactionRepository).should(times(1)).add(expectedTransaction);
            then(accountRepository).should(times(1)).update(fromAcc.deduct(amount));
        }

        @Test
        void should_lock_until_transfer_is_completed() throws Exception {
            // given
            final var initialAccount = userAccount().balance(new Money(BigDecimal.TEN)).build();
            final var accountAfterFirstTransaction = initialAccount.deduct(amount);

            final var secondTransactionAmount = new Money(BigDecimal.TWO);
            final var accountAfterSecondTransaction = accountAfterFirstTransaction.deduct(secondTransactionAmount);
            final var accountForSecondTransaction = externalAccount().externalAddress(new ExternalAddress(randomAlphabetic(8))).build();

            given(accountRepository.get(from)).willReturn(initialAccount, accountAfterFirstTransaction);

            given(accountRepository.get(to)).willAnswer(_ -> {
                Thread.sleep(500);
                return externalAccount().externalAddress(new ExternalAddress(randomAlphabetic(8))).build();
            });
            given(accountRepository.get(accountForSecondTransaction.id)).willAnswer(_ -> accountForSecondTransaction);

            final var executor = Executors.newFixedThreadPool(2);
            Callable<Long> firstTask = () -> {
                transactionManager.transferMoney(from, to, amount);
                return System.nanoTime();
            };
            Callable<Long> secondTask = () -> {
                transactionManager.transferMoney(from, accountForSecondTransaction.id, secondTransactionAmount);
                return System.nanoTime();
            };

            // when
            final var firstFinishTime = executor.submit(firstTask).get(1000, TimeUnit.MILLISECONDS);
            final var secondFinishTime = executor.submit(secondTask).get();

            // then
            executor.shutdown();
            assertThat(firstFinishTime).isLessThan(secondFinishTime);
            then(accountRepository).should(times(1)).update(accountAfterFirstTransaction);
            then(accountRepository).should(times(1)).update(accountAfterSecondTransaction);
        }
    }

    @Nested
    class CompleteTransactionTest {

        @Test
        void should_complete_transaction_and_move_funds() {
            // given
            given(accountRepository.get(from)).willReturn(userAccount().build());

            final var toAcc = userAccount().build();
            given(accountRepository.get(to)).willReturn(toAcc);

            final var transaction = transaction()
                .amount(amount)
                .state(TransactionState.PENDING)
                .type(TransactionType.INTERNAL)
                .from(from)
                .to(to)
                .build();
            given(transactionRepository.get(transaction.id())).willReturn(transaction);

            // when
            transactionManager.completeTransaction(transaction.id());

            // then
            then(transactionRepository).should(times(1)).update(transaction.complete());
            then(accountRepository).should(times(1)).update(toAcc.add(amount));
        }
    }

    @Nested
    class FailTransactionTest {
        @Test
        void should_fail_transaction_and_return_funds() {
            // given
            final var fromAcc = userAccount().build();
            given(accountRepository.get(from)).willReturn(fromAcc);

            given(accountRepository.get(to)).willReturn(userAccount().build());

            final var transaction = transaction()
                .amount(amount)
                .state(TransactionState.PENDING)
                .type(TransactionType.INTERNAL)
                .from(from)
                .to(to)
                .build();
            given(transactionRepository.get(transaction.id())).willReturn(transaction);

            // when
            transactionManager.failTransaction(transaction.id());

            // then
            then(transactionRepository).should(times(1)).update(transaction.fail());
            then(accountRepository).should(times(1)).update(fromAcc.add(amount));
        }
    }

}