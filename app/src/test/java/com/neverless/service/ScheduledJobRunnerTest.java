package com.neverless.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ScheduledJobRunnerTest {

    @Test
    void should_throw_when_scheduled_period_is_zero() {
        // then
        assertThatThrownBy(() ->
            new ScheduledJobRunner(mock(ScheduledExecutorService.class), Duration.ZERO, mock(Job.class)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_when_scheduled_period_is_negative() {
        // then
        assertThatThrownBy(() ->
            new ScheduledJobRunner(mock(ScheduledExecutorService.class), Duration.ofSeconds(1).negated(), mock(Job.class)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("argsForBuilder")
    void should_throw_when_required_args_are_null(ScheduledExecutorService executorService, Duration duration, Job job) {
        // then
        assertThatThrownBy(() ->
            new ScheduledJobRunner(executorService, duration, job))
            .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> argsForBuilder() {
        return Stream.of(
            Arguments.of(null, Duration.ofSeconds(1), mock(Job.class)),
            Arguments.of(mock(ScheduledExecutorService.class), null, mock(Job.class)),
            Arguments.of(mock(ScheduledExecutorService.class), Duration.ofSeconds(1), null)
        );
    }

}