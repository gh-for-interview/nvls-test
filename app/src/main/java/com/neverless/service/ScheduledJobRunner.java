package com.neverless.service;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class ScheduledJobRunner {
    private final ScheduledExecutorService scheduler;
    private final Duration schedulePeriod;
    private final Job job;

    public ScheduledJobRunner(ScheduledExecutorService scheduler,
                              Duration schedulePeriod,
                              Job job) {
        if (schedulePeriod.isZero() || schedulePeriod.isNegative()) {
            throw new IllegalArgumentException("Schedule period must be greater than zero");
        }

        this.scheduler = requireNonNull(scheduler);
        this.job = requireNonNull(job);
        this.schedulePeriod = requireNonNull(schedulePeriod);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
            job::run,
            Duration.ZERO.toMillis(),
            schedulePeriod.toMillis(),
            TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::close));
    }
}
