package com.neverless.service;

import com.google.common.util.concurrent.Striped;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class LockManager {
    private final Striped<Lock> locks;

    public LockManager(int lockSize) {
        this.locks = Striped.lock(lockSize);
    }

    public <T> T withLockBy(String id, Supplier<T> executable) {
        final var lock = locks.get(id);
        lock.lock();

        try {
            return executable.get();
        } finally {
            lock.unlock();
        }
    }
}
