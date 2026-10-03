package com.sit.campusbackend;

import com.sit.campusbackend.auth.security.JwtUtil;
import com.sit.campusbackend.complaint.service.CategoryDetector;
import com.sit.campusbackend.complaint.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitTests {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "The ceiling fan is not working|Electrical",
            "AC not cooling in room 4|Electrical",
            "No power in the lab|Electrical",
            "wifi keeps disconnecting|IT",
            "The printer is jammed|IT",
            "garbage near the gate|Cleaning",
            "water leaking from the tap|Plumbing",
            "mess food quality is bad|Hostel",
            "broken chair and bench|Furniture",
            "crack in the wall|Civil",
            "something unusual happened|General",
            "within the place nothing is working|General",
            "it is broken|General",
            "Fans and Lights stopped|Electrical"})
    void detectsCategoryFromWholeWordsOnly(String description, String expected) {
        assertEquals(expected, CategoryDetector.detect(description));
    }

    @Test
    void blankDescriptionIsGeneral() {
        assertEquals("General", CategoryDetector.detect(null));
        assertEquals("General", CategoryDetector.detect("   "));
    }

    @ParameterizedTest
    @CsvSource({"ELECTRICAL,Electrical", "plumbing,Plumbing", "CIVIL,Civil", "CLEANLINESS,Cleaning", "cleaning,Cleaning",
            "IT,IT", "FURNITURE,Furniture", "HOSTEL,Hostel", "OTHER,General", "General,General"})
    void mapsTheValuesTheFormSends(String picked, String expected) {
        assertEquals(expected, CategoryDetector.fromPicked(picked));
    }

    @Test
    void unknownOrBlankPickedCategoryIsNull() {
        assertNull(CategoryDetector.fromPicked(null));
        assertNull(CategoryDetector.fromPicked(""));
        assertNull(CategoryDetector.fromPicked("Teleportation"));
    }

    @Test
    void imageTypesAreDetectedFromContent() throws Exception {
        Method m = ImageStorageService.class.getDeclaredMethod("detectExtension", byte[].class);
        m.setAccessible(true);
        assertEquals("jpg", m.invoke(null, (Object) new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertEquals("png", m.invoke(null, (Object) new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0}));
        assertEquals("webp", m.invoke(null, (Object) "RIFF....WEBP".getBytes()));
        assertNull(m.invoke(null, (Object) "GIF89a......".getBytes()));
        assertNull(m.invoke(null, (Object) "<svg></svg>".getBytes()));
        assertNull(m.invoke(null, (Object) new byte[0]));
    }

    @Test
    void jwtSecretMustBeLongEnough() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short", 1000));
        JwtUtil ok = new JwtUtil("x".repeat(32), 60_000);
        assertEquals("a@b.c", ok.parse(ok.generateToken("a@b.c", "STUDENT")).getSubject());
    }

    @Test
    void expiredAndForeignTokensAreRejected() {
        JwtUtil ok = new JwtUtil("x".repeat(32), 60_000);
        JwtUtil other = new JwtUtil("y".repeat(32), 60_000);
        JwtUtil expired = new JwtUtil("x".repeat(32), -1000);
        assertThrows(io.jsonwebtoken.JwtException.class, () -> ok.parse(other.generateToken("a@b.c", "ADMIN")));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> ok.parse(expired.generateToken("a@b.c", "ADMIN")));
    }
}
