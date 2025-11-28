package com.maliroso.url_shortener.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ShortenUrlRequestTest {

    @Test
    void record_shouldHaveCorrectStructure() {
        // Given
        String url = "https://example.com";

        // When
        ShortenUrlRequest request = new ShortenUrlRequest(url);

        // Then
        assertEquals(url, request.longUrl());
    }

    @Test
    void longUrl_field_shouldHaveNotBlankAnnotation() throws NoSuchFieldException {
        Field field = ShortenUrlRequest.class.getDeclaredField("longUrl");
        assertTrue(field.isAnnotationPresent(NotBlank.class));
        NotBlank notBlank = field.getAnnotation(NotBlank.class);
        assertEquals("Long URL cannot be blank", notBlank.message());
    }

    @Test
    void longUrl_field_shouldHaveUrlAnnotation() throws NoSuchFieldException {
        Field field = ShortenUrlRequest.class.getDeclaredField("longUrl");
        assertTrue(field.isAnnotationPresent(URL.class));
        URL urlAnnotation = field.getAnnotation(URL.class);
        assertEquals("Invalid URL format", urlAnnotation.message());
    }

    @Test
    void record_shouldBeCanonical() throws Exception {
        // Verify the canonical constructor exists and works
        Constructor<ShortenUrlRequest> constructor = ShortenUrlRequest.class.getConstructor(String.class);
        ShortenUrlRequest request = constructor.newInstance("https://test.com");
        assertNotNull(request);
        assertEquals("https://test.com", request.longUrl());
    }
}