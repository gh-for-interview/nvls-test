package com.neverless.spec;

import com.neverless.App;
import com.neverless.domain.Money;
import com.neverless.domain.account.AccountRepository;
import com.neverless.storage.AccountRepositoryInMem;
import com.neverless.domain.transaction.TransactionRepository;
import com.neverless.storage.TransactionRepositoryInMem;
import com.neverless.spec.stubs.WithdrawalServiceStub;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

@ExtendWith(FunctionalSpec.ApplicationExtension.class)
public abstract class FunctionalSpec {

    protected final App application;
    protected final WithdrawalServiceStub<Money> withdrawalService;
    protected final TransactionRepository transactionRepository;
    protected final AccountRepository accountRepository;

    private final RequestSpecification specification;

    protected FunctionalSpec(ApplicationContext context) {
        this.application = context.app;
        this.withdrawalService = context.withdrawalService;
        this.accountRepository = context.accountRepository;
        this.transactionRepository = context.transactionRepository;
        this.specification = new RequestSpecBuilder()
            .setPort(context.app.port())
            .setBaseUri("http://localhost/")
            .build();
    }

    protected RequestSpecification given() {
        return RestAssured.given(specification);
    }

    protected RequestSpecification when() {
        return given().when();
    }

    public final static class ApplicationExtension implements ParameterResolver {
        private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("neverless-application-spec");

        @Override
        public boolean supportsParameter(ParameterContext params, ExtensionContext context) throws ParameterResolutionException {
            return params.getParameter().getType().equals(ApplicationContext.class);
        }

        @Override
        public Object resolveParameter(ParameterContext params, ExtensionContext context) throws ParameterResolutionException {
            final var store = context.getRoot().getStore(NAMESPACE);
            return store.getOrComputeIfAbsent(ApplicationContext.class);
        }
    }

    public final static class ApplicationContext implements ExtensionContext.Store.CloseableResource {

        public final App app;
        public final WithdrawalServiceStub<Money> withdrawalService = new WithdrawalServiceStub<>();
        public final TransactionRepository transactionRepository = new TransactionRepositoryInMem();
        public final AccountRepository accountRepository = new AccountRepositoryInMem();

        public ApplicationContext() {
            app = new App(withdrawalService, accountRepository, transactionRepository);
            app.start(0);
        }

        @Override
        public void close() throws Throwable {
            app.stop();
        }
    }
}
