package com.sit.campusbackend.auth.service;

import com.sit.campusbackend.complaint.exception.ApiException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time codes for email verification. State is kept in memory (single instance), which is fine for this app:
 * a restart only means a user has to request a new code.
 */
@Service
public class OtpService {

    static final Duration TTL = Duration.ofMinutes(10);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    static final Duration VERIFIED_WINDOW = Duration.ofMinutes(15);
    static final int MAX_ATTEMPTS = 5;

    private static final class Pending {
        final String code;
        final Instant expiresAt;
        final Instant sentAt;
        int attempts;

        Pending(String code, Instant sentAt) {
            this.code = code;
            this.sentAt = sentAt;
            this.expiresAt = sentAt.plus(TTL);
        }
    }

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> verified = new ConcurrentHashMap<>();

    public OtpService(Clock clock) {
        this.clock = clock;
    }

    /** Creates a fresh 6-digit code for the email. Throws 429 when one was sent very recently. */
    public String issue(String email) {
        Instant now = clock.instant();
        prune(now);

        Pending previous = pending.get(email);
        if (previous != null) {
            Instant retryAt = previous.sentAt.plus(RESEND_COOLDOWN);
            if (now.isBefore(retryAt)) {
                long wait = Duration.between(now, retryAt).toSeconds() + 1;
                throw ApiException.tooManyRequests("Please wait " + wait + " seconds before requesting another code.");
            }
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        pending.put(email, new Pending(code, now));
        verified.remove(email);
        return code;
    }

    /** Drops an issued code (used when the email could not be sent). */
    public void revoke(String email) {
        pending.remove(email);
    }

    /** Checks a code. On success the email is marked verified for a short window so a password can be set. */
    public void verify(String email, String code) {
        Instant now = clock.instant();
        Pending p = pending.get(email);
        if (p == null) throw new IllegalArgumentException("No OTP found. Register first.");

        synchronized (p) {
            if (now.isAfter(p.expiresAt)) {
                pending.remove(email, p);
                throw new IllegalArgumentException("OTP expired. Please request a new one.");
            }
            if (p.attempts >= MAX_ATTEMPTS) {
                pending.remove(email, p);
                throw new IllegalArgumentException("Too many wrong attempts. Please request a new OTP.");
            }
            p.attempts++;
            boolean match = code != null && MessageDigest.isEqual(
                    p.code.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
            if (!match) {
                if (p.attempts >= MAX_ATTEMPTS) pending.remove(email, p);
                throw new IllegalArgumentException("Wrong OTP.");
            }
            pending.remove(email, p);
        }
        verified.put(email, now.plus(VERIFIED_WINDOW));
    }

    public boolean isVerified(String email) {
        Instant until = verified.get(email);
        return until != null && clock.instant().isBefore(until);
    }

    public void clearVerified(String email) {
        verified.remove(email);
    }

    private void prune(Instant now) {
        pending.values().removeIf(p -> now.isAfter(p.expiresAt));
        verified.values().removeIf(now::isAfter);
    }
}
