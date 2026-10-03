package com.sit.campusbackend.auth.service;

import com.sit.campusbackend.complaint.exception.ApiException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Counts failed logins per account and per client address in a sliding window and blocks brute-forcing. */
@Component
public class LoginRateLimiter {

    static final Duration WINDOW = Duration.ofMinutes(15);
    static final int MAX_FAILURES_PER_ACCOUNT = 8;
    static final int MAX_FAILURES_PER_ADDRESS = 40;

    private static final class Counter {
        Instant windowStart;
        int failures;
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Throws 429 when either the account or the address has too many recent failures. */
    public void checkAllowed(String account, String address) {
        if (count("a:" + account) >= MAX_FAILURES_PER_ACCOUNT || count("ip:" + address) >= MAX_FAILURES_PER_ADDRESS) {
            throw ApiException.tooManyRequests("Too many failed login attempts. Try again in a few minutes.");
        }
    }

    public void recordFailure(String account, String address) {
        bump("a:" + account);
        bump("ip:" + address);
    }

    public void recordSuccess(String account) {
        counters.remove("a:" + account);
    }

    private int count(String key) {
        Counter c = counters.get(key);
        if (c == null) return 0;
        synchronized (c) {
            if (clock.instant().isAfter(c.windowStart.plus(WINDOW))) {
                counters.remove(key, c);
                return 0;
            }
            return c.failures;
        }
    }

    private void bump(String key) {
        Instant now = clock.instant();
        if (counters.size() > 10_000) counters.values().removeIf(c -> now.isAfter(c.windowStart.plus(WINDOW)));
        Counter c = counters.computeIfAbsent(key, k -> {
            Counter fresh = new Counter();
            fresh.windowStart = now;
            return fresh;
        });
        synchronized (c) {
            if (now.isAfter(c.windowStart.plus(WINDOW))) {
                c.windowStart = now;
                c.failures = 0;
            }
            c.failures++;
        }
    }
}
