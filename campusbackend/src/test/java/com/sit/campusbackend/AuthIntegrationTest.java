package com.sit.campusbackend;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    private void register(String email, String prn, int expectedStatus) throws Exception {
        postJson("/auth/register", prn == null ? Map.of("email", email) : Map.of("email", email, "prn", prn))
                .andExpect(status().is(expectedStatus));
    }

    // ── sign-up flow ─────────────────────────────────────────────────────────────────────────────

    @Test
    void fullSignUpFlowThenLogin() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);

        postJson("/auth/verify-otp", Map.of("email", email, "otp", lastOtp())).andExpect(status().isOk());
        postJson("/auth/set-password", Map.of("email", email, "password", STRONG_PASSWORD)).andExpect(status().isOk());

        postJson("/auth/login", Map.of("email", email, "password", STRONG_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andExpect(jsonPath("$.token").isNotEmpty());
        assertTrue(students.findById(email).orElseThrow().getIsVerified());
        assertEquals("24070126123", students.findById(email).orElseThrow().getPrn());
    }

    @Test
    void emailIsNormalisedToLowerCase() throws Exception {
        String email = uniqueEmail();
        register(email.toUpperCase(), "24070126123", 200);
        assertTrue(students.findById(email).isPresent());
    }

    @Test
    void settingAPasswordWithoutVerifyingTheOtpIsRejected() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);

        postJson("/auth/set-password", Map.of("email", email, "password", STRONG_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Verify your email")));
        assertNull(students.findById(email).orElseThrow().getPasswordHash());
    }

    @Test
    void settingAPasswordForAnUnknownEmailIsRejected() throws Exception {
        postJson("/auth/set-password", Map.of("email", uniqueEmail(), "password", STRONG_PASSWORD))
                .andExpect(status().isBadRequest());
    }

    @Test
    void existingAccountCannotBeTakenOver() throws Exception {
        String email = uniqueEmail();
        student(email, "Original#1", true);

        postJson("/auth/set-password", Map.of("email", email, "password", "Hacked#123")).andExpect(status().isBadRequest());
        register(email, "24070126123", 400); // "User already exists", and no OTP is issued for it

        Mockito.verifyNoInteractions(mailSender);
        postJson("/auth/login", Map.of("email", email, "password", "Original#1")).andExpect(status().isOk());
        postJson("/auth/login", Map.of("email", email, "password", "Hacked#123")).andExpect(status().isBadRequest());
    }

    @Test
    void verifiedWindowOnlyAllowsOnePasswordSet() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);
        postJson("/auth/verify-otp", Map.of("email", email, "otp", lastOtp())).andExpect(status().isOk());
        postJson("/auth/set-password", Map.of("email", email, "password", STRONG_PASSWORD)).andExpect(status().isOk());

        postJson("/auth/set-password", Map.of("email", email, "password", "Another#Pass1")).andExpect(status().isBadRequest());
        postJson("/auth/login", Map.of("email", email, "password", STRONG_PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void weakPasswordsAreRejected() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);
        postJson("/auth/verify-otp", Map.of("email", email, "otp", lastOtp())).andExpect(status().isOk());

        postJson("/auth/set-password", Map.of("email", email, "password", "short1A")).andExpect(status().isBadRequest());
        postJson("/auth/set-password", Map.of("email", email, "password", "alllowercase")).andExpect(status().isBadRequest());
        postJson("/auth/set-password", Map.of("email", email, "password", STRONG_PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void registrationValidatesEmailDomainAndPrn() throws Exception {
        register("someone@gmail.com", "24070126123", 400);
        register(uniqueEmail(), "123", 400);
        register(uniqueEmail(), "abcdefghijk", 400);
        register(uniqueEmail(), null, 400); // a brand-new account needs a PRN
        postJson("/auth/register", Map.of("prn", "24070126123")).andExpect(status().isBadRequest());
    }

    @Test
    void resendingTheCodeKeepsTheOriginalPrn() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);
        clock.advance(Duration.ofMinutes(1));
        register(email, null, 200); // what the "Resend OTP" button sends
        assertEquals("24070126123", students.findById(email).orElseThrow().getPrn());
    }

    // ── OTP rules ────────────────────────────────────────────────────────────────────────────────

    @Test
    void wrongOtpIsRejectedAndFiveWrongGuessesBurnTheCode() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);
        String real = lastOtp();
        String wrong = real.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            postJson("/auth/verify-otp", Map.of("email", email, "otp", wrong)).andExpect(status().isBadRequest());
        }
        // even the right code no longer works: a new one must be requested
        postJson("/auth/verify-otp", Map.of("email", email, "otp", real)).andExpect(status().isBadRequest());
        assertFalse(students.findById(email).orElseThrow().getIsVerified());
    }

    @Test
    void otpExpiresAfterTenMinutes() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);
        String otp = lastOtp();

        clock.advance(Duration.ofMinutes(11));
        postJson("/auth/verify-otp", Map.of("email", email, "otp", otp))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("expired")));
    }

    @Test
    void verifiedStateExpiresToo() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);
        postJson("/auth/verify-otp", Map.of("email", email, "otp", lastOtp())).andExpect(status().isOk());

        clock.advance(Duration.ofMinutes(16));
        postJson("/auth/set-password", Map.of("email", email, "password", STRONG_PASSWORD)).andExpect(status().isBadRequest());
    }

    @Test
    void otpCanOnlyBeUsedOnce() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);
        String otp = lastOtp();
        postJson("/auth/verify-otp", Map.of("email", email, "otp", otp)).andExpect(status().isOk());
        postJson("/auth/verify-otp", Map.of("email", email, "otp", otp)).andExpect(status().isBadRequest());
    }

    @Test
    void resendingTooQuicklyIsThrottled() throws Exception {
        String email = uniqueEmail();
        register(email, "24070126123", 200);
        register(email, null, 429);
        clock.advance(Duration.ofSeconds(31));
        register(email, null, 200);
        Mockito.verify(mailSender, Mockito.times(2)).send(Mockito.any(SimpleMailMessage.class));
    }

    @Test
    void aFailedEmailSendIsReportedAndDoesNotLockTheUserOut() throws Exception {
        String email = uniqueEmail();
        Mockito.doThrow(new MailSendException("smtp down")).when(mailSender).send(Mockito.any(SimpleMailMessage.class));
        register(email, "24070126123", 503);

        Mockito.reset(mailSender);
        register(email, "24070126123", 200); // no cooldown was consumed by the failed attempt
    }

    // ── login ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void unknownAccountAndWrongPasswordLookTheSame() throws Exception {
        String email = uniqueEmail();
        student(email, STRONG_PASSWORD, true);

        String wrongPassword = postJson("/auth/login", Map.of("email", email, "password", "nope"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        String unknown = postJson("/auth/login", Map.of("email", uniqueEmail(), "password", "nope"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();

        assertEquals(json.readTree(wrongPassword).get("error"), json.readTree(unknown).get("error"));
    }

    @Test
    void repeatedFailuresAreRateLimitedPerAccount() throws Exception {
        String email = uniqueEmail();
        student(email, STRONG_PASSWORD, true);

        for (int i = 0; i < 8; i++) {
            postJson("/auth/login", Map.of("email", email, "password", "wrong" + i)).andExpect(status().isBadRequest());
        }
        // locked out, even with the right password
        postJson("/auth/login", Map.of("email", email, "password", STRONG_PASSWORD)).andExpect(status().isTooManyRequests());

        clock.advance(Duration.ofMinutes(16));
        postJson("/auth/login", Map.of("email", email, "password", STRONG_PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void successfulLoginResetsTheFailureCount() throws Exception {
        String email = uniqueEmail();
        student(email, STRONG_PASSWORD, true);
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 5; i++) {
                postJson("/auth/login", Map.of("email", email, "password", "wrong")).andExpect(status().isBadRequest());
            }
            postJson("/auth/login", Map.of("email", email, "password", STRONG_PASSWORD)).andExpect(status().isOk());
        }
    }

    @Test
    void disabledStudentCannotLogInAndTheirTokenStopsWorking() throws Exception {
        String email = uniqueEmail();
        student(email, STRONG_PASSWORD, true);
        String token = login(email, STRONG_PASSWORD);
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        var s = students.findById(email).orElseThrow();
        s.setIsVerified(false);
        students.save(s);

        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        postJson("/auth/login", Map.of("email", email, "password", STRONG_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("disabled")));
    }

    @Test
    void deletedStudentsTokenStopsWorking() throws Exception {
        String email = uniqueEmail();
        student(email, STRONG_PASSWORD, true);
        String token = login(email, STRONG_PASSWORD);
        students.deleteById(email);
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminAndDepartmentLoginReturnTheirDetails() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        var d = dept("Electrical", "Dept#1234");

        postJson("/auth/login", Map.of("email", "ADMIN@sitpune.edu.in", "password", "Adm1n#Pass"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        postJson("/auth/login", Map.of("email", "electrical@sitpune.edu.in", "password", "Dept#1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DEPARTMENT"))
                .andExpect(jsonPath("$.departmentId").value(String.valueOf(d.getId())))
                .andExpect(jsonPath("$.departmentName").value("Electrical Department"));
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        postJson("/auth/login", Map.of("email", "a@sitpune.edu.in")).andExpect(status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login")
                .contentType("application/json").content("{not json")).andExpect(status().isBadRequest());
    }

    // ── tokens and roles ─────────────────────────────────────────────────────────────────────────

    @Test
    void protectedEndpointsRequireAValidToken() throws Exception {
        mvc.perform(get("/student/my-reports")).andExpect(status().isUnauthorized());
        mvc.perform(get("/student/all-reports")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/dept/queue/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());

        String email = uniqueEmail();
        student(email, STRONG_PASSWORD, true);
        String token = login(email, STRONG_PASSWORD);
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + tampered)).andExpect(status().isUnauthorized());
    }

    @Test
    void rolesAreSeparated() throws Exception {
        admin("admin@sitpune.edu.in", "Adm1n#Pass");
        dept("IT", "Dept#1234");
        String email = uniqueEmail();
        student(email, STRONG_PASSWORD, true);

        String student = login(email, STRONG_PASSWORD);
        String admin = login("admin@sitpune.edu.in", "Adm1n#Pass");
        String dept = login("it@sitpune.edu.in", "Dept#1234");

        mvc.perform(get("/admin/stats").header("Authorization", "Bearer " + student)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/stats").header("Authorization", "Bearer " + dept)).andExpect(status().isForbidden());
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + admin)).andExpect(status().isForbidden());
        mvc.perform(get("/student/my-reports").header("Authorization", "Bearer " + dept)).andExpect(status().isForbidden());
        mvc.perform(get("/dept/queue/1").header("Authorization", "Bearer " + student)).andExpect(status().isForbidden());
        mvc.perform(get("/dept/queue/1").header("Authorization", "Bearer " + admin)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/stats").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
    }

    @Test
    void unknownPathsAreNotFoundOrDeniedNeverServerErrors() throws Exception {
        mvc.perform(get("/nope")).andExpect(status().isUnauthorized());
        mvc.perform(get("/uploads/does-not-exist.png")).andExpect(status().isNotFound());
    }

    @Test
    void corsOnlyAllowsTheConfiguredOrigins() throws Exception {
        mvc.perform(options("/auth/login").header("Origin", "http://localhost:5500")
                        .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"));
        mvc.perform(options("/auth/login").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
