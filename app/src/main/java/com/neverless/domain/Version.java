package com.neverless.domain;

public record Version(int value) {
    public Version {
        if (value < 1) {
            throw new IllegalArgumentException("Version must be positive");
        }
    }

    public Version increment() {
        return new Version(this.value + 1);
    }

    public static Version firstVersion() {
        return new Version(1);
    }
}
