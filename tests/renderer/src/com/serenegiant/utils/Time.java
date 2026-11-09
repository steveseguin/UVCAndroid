package com.serenegiant.utils;

// Deterministic monotonic clock stand-in. Tests never sleep.
public final class Time {
    public static long now;
    public static long nanoTime() { return now; }
}
