package com.maliroso.url_shortener.controller;

import com.maliroso.url_shortener.dto.request.ShortenUrlRequest;
import com.maliroso.url_shortener.dto.response.ShortUrlMetadataResponse;
import com.maliroso.url_shortener.dto.response.ShortenUrlResponse;
import com.maliroso.url_shortener.model.UrlMapping;
import com.maliroso.url_shortener.service.RedirectMetricsService;
import com.maliroso.url_shortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlControllerTest {

    @Mock
    private UrlService urlService;

    @Mock
    private RedirectMetricsService metricsService;

    @InjectMocks
    private UrlController urlController;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        // Simulate @Value("${base_url}") – set it directly since we're not in Spring context
        baseUrl = "http://localhost:8080/";
        // Use reflection to set the private field (since no setter)
        try {
            java.lang.reflect.Field field = UrlController.class.getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(urlController, baseUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set baseUrl", e);
        }
    }

    // -----------------------------
    // Tests for createShortUrl
    // -----------------------------

    @Test
    void createShortUrl_returnsExistingShortUrl_whenAlreadyExists() {
        String longUrl = "http://example.com/very/long";
        String code = "abc123";
        ShortenUrlRequest request = new ShortenUrlRequest(longUrl);

        UrlMapping existingMapping = new UrlMapping();
        existingMapping.setCode(code);
        existingMapping.setLongUrl(longUrl);
        existingMapping.setExpiresAt(Instant.now().plusSeconds(3600));

        when(urlService.fetchByLongUrl(eq(longUrl), any(Instant.class)))
                .thenReturn(java.util.Optional.of(existingMapping));

        ResponseEntity<?> response = urlController.createShortUrl(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ShortenUrlResponse);
        ShortenUrlResponse body = (ShortenUrlResponse) response.getBody();
        assertEquals(code, body.code());
        assertEquals(baseUrl + "r/" + code, body.shortUrl());

        verify(urlService, never()).createShortUrlCode(any());
    }

    @Test
    void createShortUrl_createsNewShortUrl_whenNotExists() {
        String longUrl = "http://example.com/new";
        String code = "xyz789";
        ShortenUrlRequest request = new ShortenUrlRequest(longUrl);

        UrlMapping newMapping = new UrlMapping();
        newMapping.setCode(code);
        newMapping.setLongUrl(longUrl);
        newMapping.setExpiresAt(Instant.now().plusSeconds(3600));

        when(urlService.fetchByLongUrl(eq(longUrl), any(Instant.class)))
                .thenReturn(java.util.Optional.empty());
        when(urlService.createShortUrlCode(request))
                .thenReturn(newMapping);

        ResponseEntity<?> response = urlController.createShortUrl(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ShortenUrlResponse);
        ShortenUrlResponse body = (ShortenUrlResponse) response.getBody();
        assertEquals(code, body.code());
        assertEquals(baseUrl + "r/" + code, body.shortUrl());

        verify(urlService).createShortUrlCode(request);
    }

    @Test
    void createShortUrl_returnsUnprocessableContent_whenExceptionThrown() {
        ShortenUrlRequest request = new ShortenUrlRequest("http://example.com");

        when(urlService.fetchByLongUrl(anyString(), any(Instant.class)))
                .thenThrow(new RuntimeException("Simulated error"));

        ResponseEntity<?> response = urlController.createShortUrl(request);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof String);
    }

    @Test
    void redirectToUrl_returnsNotFound_whenCodeNotFound() {
        String code = "missing";

        when(urlService.fetchUrlMapping(eq(code), any(Instant.class)))
                .thenReturn(java.util.Optional.empty());

        ResponseEntity<?> response = urlController.redirectToUrl(code);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(metricsService, never()).recordRedirect();
    }

    @Test
    void redirectToUrl_returnsNotFound_whenExceptionThrown() {
        String code = "error";

        when(urlService.fetchUrlMapping(eq(code), any(Instant.class)))
                .thenThrow(new RuntimeException("Unexpected"));

        ResponseEntity<?> response = urlController.redirectToUrl(code);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(metricsService, never()).recordRedirect();
    }
}