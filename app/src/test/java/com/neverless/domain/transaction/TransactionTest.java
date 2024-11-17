package com.neverless.domain.transaction;

import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static com.neverless.domain.transaction.TransactionState.*;
import static com.neverless.domain.transaction.TransactionType.INTERNAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void should_complete_transaction() {
        // given
        final var original = transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(new Money(BigDecimal.TEN))
            .type(INTERNAL)
            .state(PENDING)
            .build();

        // when
        final var result = original.complete();

        // then
        assertThat(result.state()).isEqualTo(COMPLETED);
        assertThat(result.id()).isEqualTo(original.id());
    }

    @Test
    void should_fail_to_complete_transaction_when_state_is_not_pending() {
        // given
        final var original = transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(new Money(BigDecimal.TEN))
            .type(INTERNAL)
            .state(COMPLETED)
            .build();

        // then
        assertThatThrownBy(original::complete).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_fail_transaction() {
        // given
        final var original = transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(new Money(BigDecimal.TEN))
            .type(INTERNAL)
            .state(PENDING)
            .build();

        // when
        final var result = original.fail();

        // then
        assertThat(result.state()).isEqualTo(FAILED);
        assertThat(result.id()).isEqualTo(original.id());
    }

    @Test
    void should_fail_to_fail_transaction_when_state_is_not_pending() {
        // given
        final var original = transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(new Money(BigDecimal.TEN))
            .type(INTERNAL)
            .state(COMPLETED)
            .build();

        // then
        assertThatThrownBy(original::fail).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_throw_when_accounts_are_the_same() {
        // given
        final var acc = AccountId.random();
        final var builder = transaction()
            .from(acc)
            .to(acc)
            .amount(new Money(BigDecimal.TEN))
            .type(INTERNAL)
            .state(COMPLETED);

        // then
        assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_when_amount_is_negative() {
        // given
        final var builder = transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(new Money(BigDecimal.TEN.negate()))
            .type(INTERNAL)
            .state(COMPLETED);

        // then
        assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_when_amount_is_zero() {
        // given
        final var builder = transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(Money.zero())
            .type(INTERNAL)
            .state(COMPLETED);

        // then
        assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("argsForBuilder")
    void should_throw_when_required_field_is_null(AccountId from, AccountId to, Money amount, TransactionType type, TransactionState state) {
        // given
        final var builder = transaction()
            .from(from)
            .to(to)
            .amount(amount)
            .type(type)
            .state(state);

        // then
        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> argsForBuilder() {
        return Stream.of(
            Arguments.of(null, AccountId.random(), new Money(BigDecimal.TEN), INTERNAL, PENDING),
            Arguments.of(AccountId.random(), null, new Money(BigDecimal.TEN), INTERNAL, PENDING),
            Arguments.of(AccountId.random(), AccountId.random(), null, INTERNAL, PENDING),
            Arguments.of(AccountId.random(), AccountId.random(), new Money(BigDecimal.TEN), null, PENDING),
            Arguments.of(AccountId.random(), AccountId.random(), new Money(BigDecimal.TEN), INTERNAL, null)
        );
    }
}