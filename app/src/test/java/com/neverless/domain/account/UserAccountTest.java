package com.neverless.domain.account;

import com.neverless.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static com.neverless.domain.Money.zero;
import static com.neverless.domain.account.AccountType.INTERNAL;
import static com.neverless.domain.account.UserAccount.Builder.userAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountTest {

    @Test
    void should_add_balance() {
        // given
        final var account = userAccount().balance(new Money(BigDecimal.ONE)).build();

        // when
        final var result = account.add(new Money(BigDecimal.TEN));

        // then
        assertThat(result.balance).isEqualTo(new Money(new BigDecimal(11)));
    }

    @Test
    void should_deduct_balance() {
        // given
        final var account = userAccount().balance(new Money(BigDecimal.TEN)).build();

        // when
        final var result = account.deduct(new Money(BigDecimal.ONE));

        // then
        assertThat(result.balance).isEqualTo(new Money(new BigDecimal(9)));
    }


    @ParameterizedTest
    @MethodSource("argsForBuilder")
    void should_throw_when_required_field_is_null(AccountId id, AccountType type, Money balance) {
        // given
        var builder = userAccount()
            .id(id)
            .type(type)
            .balance(balance);

        // then
        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> argsForBuilder() {
        return Stream.of(
            Arguments.of(null, INTERNAL, zero()),
            Arguments.of(AccountId.random(), null, zero()),
            Arguments.of(AccountId.random(), INTERNAL, null)
        );
    }
}