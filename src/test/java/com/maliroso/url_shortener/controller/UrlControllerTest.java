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
        baseUrl = "https://localhost:8080/";
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
        String longUrl = "https://example.com/very/long";
        String code = "abc123";
        ShortenUrlRequest request = new ShortenUrlRequest(longUrl);

        UrlMapping existingMapping = new UrlMapping();
        existingMapping.setCode(code);
        existingMapping.setLongUrl(longUrl);
        existingMapping.setExpiresAt(Instant.now().plusSeconds(3600));

        when(urlService.fetchByLongUrl(longUrl, any(Instant.class)))
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
        String longUrl = "https://example.com/new";
        String code = "xyz789";
        ShortenUrlRequest request = new ShortenUrlRequest(longUrl);

        UrlMapping newMapping = new UrlMapping();
        newMapping.setCode(code);
        newMapping.setLongUrl(longUrl);
        newMapping.setExpiresAt(Instant.now().plusSeconds(3600));

        when(urlService.fetchByLongUrl(longUrl, any(Instant.class)))
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
        ShortenUrlRequest request = new ShortenUrlRequest("https://example.com");

        when(urlService.fetchByLongUrl(anyString(), any(Instant.class)))
                .thenThrow(new RuntimeException("Simulated error"));

        ResponseEntity<?> response = urlController.createShortUrl(request);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof String);
    }

    // ------------------------------------
    // Tests for getUrlMetadata
    // ------------------------------------

    @Test
    void getUrlMetadata_returnsMetadata_whenFound() {
        String code = "meta123";
        String longUrl = "https://example.com/meta";
        Instant createdAt = Instant.now();
        int initialHitCount = 5;

        UrlMapping mapping = new UrlMapping();
        mapping.setCode(code);
        mapping.setLongUrl(longUrl);
        mapping.setCreatedAt(createdAt);
        mapping.setHitCount(initialHitCount);
        mapping.setExpiresAt(Instant.now().plusSeconds(3600));

        when(urlService.fetchByLongUrl(code, any(Instant.class)))
                .thenReturn(java.util.Optional.of(mapping));

        ResponseEntity<?> response = urlController.getUrlMetadata(code);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ShortUrlMetadataResponse);
        ShortUrlMetadataResponse body = (ShortUrlMetadataResponse) response.getBody();
        assertEquals(code, body.code());
        assertEquals(baseUrl + "r/" + code, body.shortUrl());
        assertEquals(longUrl, body.longUrl());
        assertEquals(createdAt, body.createdAt());
        assertEquals(initialHitCount, body.hitCount()); // note: response uses original count

        // Verify hit count was incremented and saved
        verify(urlService).updateHitCount(argThat(m ->
                m.getCode().equals(code) && m.getHitCount() == initialHitCount + 1
        ));
    }

    @Test
    void getUrlMetadata_returnsNotFound_whenNotExists() {
        String code = "notfound";

        when(urlService.fetchByLongUrl(code, any(Instant.class)))
                .thenReturn(java.util.Optional.empty());

        ResponseEntity<?> response = urlController.getUrlMetadata(code);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getUrlMetadata_returnsUnprocessableContent_whenExceptionThrown() {
        String code = "error123";

        when(urlService.fetchByLongUrl(code, any(Instant.class)))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = urlController.getUrlMetadata(code);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ------------------------------------
    // Tests for redirectToUrl
    // ------------------------------------

    @Test
    void redirectToUrl_redirectsSuccessfully_whenFound() {
        String code = "redir456";
        String longUrl = "https://example.com/target";

        UrlMapping mapping = new UrlMapping();
        mapping.setCode(code);
        mapping.setLongUrl(longUrl);
        mapping.setHitCount(10);
        mapping.setExpiresAt(Instant.now().plusSeconds(3600));

        when(urlService.fetchUrlMapping(code, any(Instant.class)))
                .thenReturn(java.util.Optional.of(mapping));

        ResponseEntity<?> response = urlController.redirectToUrl(code);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        HttpHeaders headers = response.getHeaders();
        assertNotNull(headers.getLocation());
        assertEquals(URI.create(longUrl), headers.getLocation());

        // Verify hit count incremented and saved
        verify(urlService).updateHitCount(argThat(m ->
                m.getCode().equals(code) && m.getHitCount() == 11
        ));
        verify(metricsService).recordRedirect();
    }

    @Test
    void redirectToUrl_addsHttpPrefix_whenLongUrlDoesNotStartWithHttp() {
        String code = "nohttp";
        String longUrl = "example.com"; // no protocol

        UrlMapping mapping = new UrlMapping();
        mapping.setCode(code);
        mapping.setLongUrl(longUrl);
        mapping.setHitCount(0);
        mapping.setExpiresAt(Instant.now().plusSeconds(3600));

        when(urlService.fetchUrlMapping(code, any(Instant.class)))
                .thenReturn(java.util.Optional.of(mapping));

        ResponseEntity<?> response = urlController.redirectToUrl(code);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        URI location = response.getHeaders().getLocation();
        assertNotNull(location);
        assertEquals("http://example.com", location.toString());

        verify(urlService).updateHitCount(any(UrlMapping.class));
        verify(metricsService).recordRedirect();
    }

    @Test
    void redirectToUrl_returnsNotFound_whenCodeNotFound() {
        String code = "missing";

        when(urlService.fetchUrlMapping(code, any(Instant.class)))
                .thenReturn(java.util.Optional.empty());

        ResponseEntity<?> response = urlController.redirectToUrl(code);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(metricsService, never()).recordRedirect();
    }

    @Test
    void redirectToUrl_returnsNotFound_whenExceptionThrown() {
        String code = "error";

        when(urlService.fetchUrlMapping(code, any(Instant.class)))
                .thenThrow(new RuntimeException("Unexpected"));

        ResponseEntity<?> response = urlController.redirectToUrl(code);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(metricsService, never()).recordRedirect();
    }
}