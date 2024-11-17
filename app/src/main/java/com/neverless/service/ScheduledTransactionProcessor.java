package com.neverless.service;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class ScheduledTransactionProcessor {
    private final ScheduledExecutorService scheduler;
    private final Duration schedulePeriod;
    private final TransactionProcessor transactionProcessor;

    public ScheduledTransactionProcessor(ScheduledExecutorService scheduler,
                                         Duration schedulePeriod,
                                         TransactionProcessor transactionProcessor) {
        if (schedulePeriod.isZero() || schedulePeriod.isNegative()) {
            throw new IllegalArgumentException("Schedule period must be greater than zero");
        }

        this.scheduler = requireNonNull(scheduler);
        this.transactionProcessor = requireNonNull(transactionProcessor);
        this.schedulePeriod = requireNonNull(schedulePeriod);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
            transactionProcessor::process,
            Duration.ZERO.toMillis(),
            schedulePeriod.toMillis(),
            TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::close));
    }
}
