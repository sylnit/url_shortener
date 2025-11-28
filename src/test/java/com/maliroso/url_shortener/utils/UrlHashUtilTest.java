package com.maliroso.url_shortener.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class UrlHashUtilTest {

    @Test
    void generateMd5Hash_returnsSixCharacterBase62String() {
        String hash = UrlHashUtil.generateMd5Hash("https://linkedin.com");

        assertNotNull(hash);
        assertEquals(6, hash.length());

        String allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (char c : hash.toCharArray()) {
            assertTrue(allowedChars.indexOf(c) >= 0);
        }
    }

    @Test
    void generateMd5Hash_returnsDifferentValuesOnSuccessiveCalls() {
        String hash1 = UrlHashUtil.generateMd5Hash("https://linkedin.com");
        String hash2 = UrlHashUtil.generateMd5Hash("https://linkedin.com");

        assertNotEquals(hash1, hash2);
    }


    @Test
    void calculateExpiresAt_returnsInstantExactlySevenDaysFromNow() {
        Instant fixedNow = Instant.parse("2023-01-01T00:00:00Z");

        try (MockedStatic<Instant> mockedInstant = mockStatic(Instant.class)) {
            mockedInstant.when(Instant::now).thenReturn(fixedNow);

            Instant expiresAt = UrlHashUtil.calculateExpiresAt();
            Instant expected = fixedNow.plus(Duration.ofDays(7));

            assertEquals(expected, expiresAt);
        }
    }
}