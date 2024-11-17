package com.neverless.domain.account;

import com.neverless.domain.ExternalAddress;
import com.neverless.domain.Money;
import com.neverless.domain.Version;
import com.neverless.exceptions.NotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static com.neverless.domain.account.ExternalAccount.Builder.externalAccount;
import static com.neverless.domain.account.UserAccount.Builder.userAccount;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountRepositoryInMemTest {
    AccountRepositoryInMem repository = new AccountRepositoryInMem();

    @Nested
    class FindTest {
        @Test
        void should_return_account_if_it_exists() {
            // given
            var acc = repository.add(userAccount().build());

            // when
            var result = repository.find(acc.id);

            // then
            assertThat(result).contains(acc);
        }

        @Test
        void should_return_empty_account_if_account_not_found() {
            // when
            var result = repository.find(AccountId.random());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByExternalAddressTest {
        @Test
        void should_return_account_if_it_exists() {
            // given
            var externalAddress = new ExternalAddress(randomAlphabetic(8));
            var acc = externalAccount().externalAddress(externalAddress).build();
            repository.add(acc);

            // when
            var result = repository.find(externalAddress);

            // then
            assertThat(result).contains(acc);
        }

        @Test
        void should_return_empty_account_if_account_not_found() {
            // when
            var result = repository.find(new ExternalAddress(randomAlphabetic(8)));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetTest {
        @Test
        void should_return_account_if_it_exists() {
            // given
            var acc = repository.add(userAccount().build());

            // when
            var result = repository.get(acc.id);

            // then
            assertThat(result).isEqualTo(acc);
        }

        @Test
        void should_throw_if_account_does_not_exists() {
            // then
            assertThatThrownBy(() -> repository.get(AccountId.random())).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class UpdateTest {
        @Test
        void should_update_account_if_it_exists() {
            // given
            var acc = repository.add(userAccount().build());
            var updatedAcc = acc.add(new Money(BigDecimal.TEN));

            // when
            var result = repository.update(updatedAcc);

            // then
            assertThat(result).isEqualTo(updatedAcc);
        }

        @Test
        void should_throw_if_transaction_is_not_exactly_one_version_higher() {
            // given
            var acc = repository.add(userAccount().build());
            var unmatchedByVersionAcc = userAccount()
                .id(acc.id)
                .balance(acc.balance)
                .type(acc.type)
                .version(new Version(10))
                .build();

            // then
            assertThatThrownBy(() -> repository.update(unmatchedByVersionAcc)).isInstanceOf(ConcurrentModificationException.class);
        }

        @Test
        void should_throw_if_account_does_not_exists() {
            // then
            assertThatThrownBy(() -> repository.update(userAccount().build())).isInstanceOf(NotFoundException.class);
        }
    }

}