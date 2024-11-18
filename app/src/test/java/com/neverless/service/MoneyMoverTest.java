package com.neverless.service;

import com.neverless.domain.account.ExternalAddress;
import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.account.AccountRepository;
import com.neverless.domain.transaction.ExternalRef;
import com.neverless.domain.transaction.TransactionRepository;
import com.neverless.domain.transaction.TransactionState;
import com.neverless.domain.transaction.TransactionType;
import com.neverless.exceptions.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;

import static com.neverless.domain.account.ExternalAccount.Builder.externalAccount;
import static com.neverless.domain.account.UserAccount.Builder.userAccount;
import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class MoneyMoverTest {
    TransactionRepository transactionRepository = mock(TransactionRepository.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    LockManager lockManager = mock(LockManager.class);
    MoneyMover moneyMover = new MoneyMover(transactionRepository, accountRepository, lockManager);

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

    @Nested
    class MoveMoneyTest {
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
            assertThatThrownBy(() -> moneyMover.moveMoney(from, to, new Money(BigDecimal.ONE)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_amount_is_negative() {
            // then
            assertThatThrownBy(() -> moneyMover.moveMoney(from, to, new Money(BigDecimal.TEN.negate())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_amount_is_zero() {
            // then
            assertThatThrownBy(() -> moneyMover.moveMoney(from, to, Money.zero()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_account_is_the_same() {
            // then
            assertThatThrownBy(() -> moneyMover.moveMoney(from, from, amount))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_account_has_insufficient_balance() {
            // given
            given(accountRepository.get(from)).willReturn(userAccount().balance(Money.zero()).build());
            given(accountRepository.get(to)).willReturn(userAccount().build());

            // then
            assertThatThrownBy(() -> moneyMover.moveMoney(from, to, amount))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        void should_create_transaction_and_lock_funds() {
            // given
            final var fromAcc = userAccount().balance(new Money(BigDecimal.TEN)).build();
            given(accountRepository.get(from)).willReturn(fromAcc);
            given(accountRepository.get(to)).willReturn(userAccount().build());

            // when
            final var result = moneyMover.moveMoney(from, to, amount);

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
            final var result = moneyMover.moveMoney(from, to, amount, Optional.of(externalRef));

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
            final var result = moneyMover.moveMoney(from, to, amount);

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
    }

    @Nested
    class AddMoneyTest {

        @Test
        void should_throw_when_amount_is_negative() {
            // then
            assertThatThrownBy(() -> moneyMover.addMoney(from, new Money(BigDecimal.TEN.negate())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_when_amount_is_zero() {
            // then
            assertThatThrownBy(() -> moneyMover.addMoney(from, Money.zero()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_add_money() {
            // given
            final var acc = userAccount().build();
            given(accountRepository.get(acc.id)).willReturn(acc);

            // when
            moneyMover.addMoney(acc.id, amount);

            // then
            then(accountRepository).should(times(1)).update(acc.add(amount));
        }

    }
}