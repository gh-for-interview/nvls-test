package com.neverless.spec;

import com.neverless.domain.ExternalAddress;
import com.neverless.domain.Money;
import com.neverless.domain.account.AccountId;
import com.neverless.domain.transaction.TransactionId;
import com.neverless.integration.WithdrawalService.WithdrawalId;
import com.neverless.spec.stubs.ControlledWithdrawalServiceStub;
import io.restassured.response.Response;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.neverless.domain.Money.zero;
import static com.neverless.domain.account.ExternalAccount.Builder.externalAccount;
import static com.neverless.domain.account.UserAccount.Builder.userAccount;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WithdrawalsSpec extends FunctionalSpec {

    protected WithdrawalsSpec(ApplicationContext context) {
        super(context);
    }

    ControlledWithdrawalServiceStub<Money> withdrawalService = (ControlledWithdrawalServiceStub) application.withdrawalService;

    @Nested
    class WithdrawalsRequestSpec {
        @Test
        void should_request_withdrawal() {
            // given
            final var fromAccount = setupAccount();
            final var externalAddress = setupExternalAddress();

            // when
            final var response = when().body(requestBody(fromAccount, externalAddress))
                .post("/withdrawal")
                .thenReturn();

            // then
            validateCreatedResponse(response);
        }

        @Test
        void should_request_withdrawal_and_freeze_funds_until_request_is_finalised() {
            // given
            final var fromAccount = setupAccount();
            final var externalAddress = setupExternalAddress();

            // when
            final var firstResponse = when().body(requestBody(fromAccount, externalAddress, 7))
                .post("/withdrawal")
                .thenReturn();

            // then
            final var responseId = validateCreatedResponse(firstResponse);

            // when
            final var secondResponse = when().body(requestBody(fromAccount, externalAddress, 7))
                .post("/withdrawal")
                .thenReturn();

            // then
            assertThat(secondResponse.statusCode()).isEqualTo(400);

            // when
            final var txn = application.transactionRepository.get(new TransactionId(responseId));
            withdrawalService.fail(new WithdrawalId(UUID.fromString(txn.externalRef().get().value())));

            // then
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                final var thirdResponse = when().body(requestBody(fromAccount, externalAddress, 7))
                    .post("/withdrawal")
                    .thenReturn();

                if (thirdResponse.statusCode() == 201) {
                    validateCreatedResponse(thirdResponse);
                    return true;
                } else {
                    return false;
                }
            });
        }

        @Test
        void should_request_withdrawal_for_2_parallel_requests() {
            // given
            final var fromAccount = setupAccount();
            final var externalAddress = setupExternalAddress();

            // when
            final var firstResponse = when().body(requestBody(fromAccount, externalAddress))
                .post("/withdrawal")
                .thenReturn();

            // then
            final var firstResponseId = validateCreatedResponse(firstResponse);

            // when
            final var secondResponse = when().body(requestBody(fromAccount, externalAddress))
                .post("/withdrawal")
                .thenReturn();

            // then
            final var secondResponseId = validateCreatedResponse(secondResponse);

            assertThat(firstResponseId).isNotEqualTo(secondResponseId);
        }

        @Test
        void should_return_400_when_balance_is_insufficient() {
            // given
            final var fromAccount = application.accountRepository.add(userAccount().balance(zero()).build());
            final var externalAddress = setupExternalAddress();

            // when
            final var response = when().body(requestBody(fromAccount.id, externalAddress))
                .post("/withdrawal")
                .thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(400);
        }

        @Test
        void should_return_404_when_account_does_not_exists() {
            // given
            final var externalAddress = setupExternalAddress();

            // when
            final var response = when().body(requestBody(AccountId.random(), externalAddress, 1))
                .post("/withdrawal")
                .thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void should_return_404_when_address_does_not_exists() {
            // given
            final var fromAccount = setupAccount();

            // when
            final var response = when().body(requestBody(fromAccount, new ExternalAddress("non-existent-address")))
                .post("/withdrawal")
                .thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void should_return_400_for_malformed_input() {
            // when
            final var response = when().body("xyz")
                .post("/withdrawal")
                .thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(400);
        }

        private UUID validateCreatedResponse(Response response) {
            assertThat(response.statusCode()).isEqualTo(201);
            final var responseId = responseId(response);
            assertThat(withdrawalService.getRequestState(withdrawalId(responseId))).isNotNull();
            return responseId;
        }
    }

    @Nested
    class WithdrawalsStatusSpec {
        @Test
        void should_return_processing_state() {
            // given
            final var account = setupAccount();
            final var externalAddress = setupExternalAddress();
            final var creationResponse = when().body(requestBody(account, externalAddress))
                .post("/withdrawal")
                .thenReturn();
            final var txnId = responseId(creationResponse);

            // when
            final var response = when().get("/withdrawal/{id}/state", txnId.toString()).thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThatJson(response.body().asString()).isEqualTo(
                """
                {
                    "state": "PROCESSING"
                }
                """
            );
        }

        @Test
        void should_return_complete_state() {
            // given
            final var account = setupAccount();
            final var externalAddress = setupExternalAddress();
            final var creationResponse = when().body(requestBody(account, externalAddress))
                .post("/withdrawal")
                .thenReturn();
            final var txnId = responseId(creationResponse);

            // when
            withdrawalService.complete(withdrawalId(txnId));

            final var response = when().get("/withdrawal/{id}/state", txnId.toString()).thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThatJson(response.body().asString()).isEqualTo(
                    """
                    {
                        "state": "COMPLETED"
                    }
                    """
            );
        }

        @Test
        void should_return_failed_state() {
            // given
            final var account = setupAccount();
            final var externalAddress = setupExternalAddress();
            final var creationResponse = when().body(requestBody(account, externalAddress))
                    .post("/withdrawal")
                    .thenReturn();
            final var txnId = responseId(creationResponse);

            // when
            withdrawalService.fail(withdrawalId(txnId));

            final var response = when().get("/withdrawal/{id}/state", txnId.toString()).thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(200);
            assertThatJson(response.body().asString()).isEqualTo(
                    """
                    {
                        "state": "FAILED"
                    }
                    """
            );
        }

        @Test
        void should_return_404_when_withdrawal_does_not_exists() {
            // given
            final var id = TransactionId.random();

            // when
            final var response = when().get("/withdrawal/{id}/state", id.value()).thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(404);
        }

        @Test
        void should_return_400_for_malformed_input() {
            // when
            final var response = when().get("/withdrawal/xyz/state").thenReturn();

            // then
            assertThat(response.statusCode()).isEqualTo(400);
        }
    }

    private WithdrawalId withdrawalId(UUID id) {
        final var txn = application.transactionRepository.get(new TransactionId(id));
        return new WithdrawalId(UUID.fromString(txn.externalRef().get().value()));
    }

    private String requestBody(AccountId from, ExternalAddress to) {
        return requestBody(from.value(), to.value(), 1);
    }

    private String requestBody(AccountId from, ExternalAddress to, int amount) {
        return requestBody(from.value(), to.value(), amount);
    }

    private String requestBody(UUID from, String to, int amount) {
        return """
                {
                    "amount" : %s,
                    "fromAccount" : "%s",
                    "toAddress" : "%s"
                }""".formatted(amount, from, to);
    }

    private AccountId setupAccount() {
        return application.accountRepository.add(userAccount().balance(new Money(BigDecimal.TEN)).build()).id;
    }

    private ExternalAddress setupExternalAddress() {
        final var externalAddress = new ExternalAddress(randomAlphabetic(12)); // random
        application.accountRepository.add(externalAccount()
            .externalAddress(externalAddress)
            .build());
        return externalAddress;
    }

    private UUID responseId(Response response) {
        return response.body().jsonPath().getUUID("id");
    }
}
