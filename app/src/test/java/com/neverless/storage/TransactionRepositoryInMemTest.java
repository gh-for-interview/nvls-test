package com.neverless.storage;

import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.transaction.Transaction;
import com.neverless.domain.transaction.TransactionId;
import com.neverless.exceptions.NotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;


import static com.neverless.domain.transaction.Transaction.Builder.transaction;
import static com.neverless.domain.transaction.TransactionState.COMPLETED;
import static com.neverless.domain.transaction.TransactionState.PENDING;
import static com.neverless.domain.transaction.TransactionType.EXTERNAL;
import static com.neverless.domain.transaction.TransactionType.INTERNAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRepositoryInMemTest {
    TransactionRepositoryInMem repository = new TransactionRepositoryInMem();

    @Nested
    class FindTest {
        @Test
        void should_return_transaction_if_it_exists() {
            // given
            var txn = repository.add(aTransaction());

            // when
            var result = repository.find(txn.id());

            // then
            assertThat(result).contains(txn);
        }

        @Test
        void should_return_empty_transaction_if_transaction_not_found() {
            // when
            var result = repository.find(TransactionId.random());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByTypeAndStateTest {
        @Test
        void should_return_transaction_if_it_exists() {
            // given
            var txn = repository.add(aTransaction());

            // when
            var result = repository.find(txn.type(), txn.state());

            // then
            assertThat(result.size()).isGreaterThanOrEqualTo(1);
            assertThat(result).contains(txn);
        }

        @Test
        void should_return_empty_transaction_if_transaction_not_found() {
            // when
            var result = repository.find(EXTERNAL, COMPLETED);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetTest {
        @Test
        void should_return_transaction_if_it_exists() {
            // given
            var txn = repository.add(aTransaction());

            // when
            var result = repository.get(txn.id());

            // then
            assertThat(result).isEqualTo(txn);
        }

        @Test
        void should_throw_if_transaction_does_not_exists() {
            // then
            assertThatThrownBy(() -> repository.get(TransactionId.random())).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class UpdateTest {
        @Test
        void should_update_transaction_if_it_exists() {
            // given
            var txn = repository.add(aTransaction());
            var updatedTxn = txn.complete();

            // when
            var result = repository.update(updatedTxn);

            // then
            assertThat(result).isEqualTo(updatedTxn);
        }

        @Test
        void should_throw_if_transaction_does_not_exists() {
            // then
            assertThatThrownBy(() -> repository.update(aTransaction())).isInstanceOf(NotFoundException.class);
        }
    }
    
    private Transaction aTransaction() {
        return transaction()
            .from(AccountId.random())
            .to(AccountId.random())
            .amount(new Money(BigDecimal.TEN))
            .type(INTERNAL)
            .state(PENDING)
            .build();
    }
}