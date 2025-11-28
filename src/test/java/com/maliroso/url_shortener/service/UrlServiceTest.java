package com.maliroso.url_shortener.service;

import com.maliroso.url_shortener.dto.request.ShortenUrlRequest;
import com.maliroso.url_shortener.model.UrlMapping;
import com.maliroso.url_shortener.repository.UrlMappingRepository;
import com.maliroso.url_shortener.utils.UrlHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @InjectMocks
    private UrlService urlService;

    private Instant now;
    private String longUrl;
    private String hash;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        longUrl = "https://linkedin.com";
        hash = "abc123";
    }

    @Test
    void fetchUrlMapping_returnsMapping_whenFound() {
        UrlMapping mapping = new UrlMapping();
        mapping.setCode(hash);
        mapping.setLongUrl(longUrl);
        mapping.setExpiresAt(now.plusSeconds(100));

        when(urlMappingRepository.findByCodeAndExpiresAtGreaterThanEqual(hash, now))
                .thenReturn(Optional.of(mapping));

        Optional<UrlMapping> result = urlService.fetchUrlMapping(hash, now);

        assertTrue(result.isPresent());
        assertEquals(hash, result.get().getCode());
        verify(urlMappingRepository).findByCodeAndExpiresAtGreaterThanEqual(hash, now);
    }

    @Test
    void fetchUrlMapping_returnsEmpty_whenNotFound() {
        when(urlMappingRepository.findByCodeAndExpiresAtGreaterThanEqual(hash, now))
                .thenReturn(Optional.empty());

        Optional<UrlMapping> result = urlService.fetchUrlMapping(hash, now);

        assertFalse(result.isPresent());
        verify(urlMappingRepository).findByCodeAndExpiresAtGreaterThanEqual(hash, now);
    }

    @Test
    void fetchByLongUrl_returnsMapping_whenFound() {
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl(longUrl);
        mapping.setExpiresAt(now.plusSeconds(100));

        when(urlMappingRepository.findByLongUrlAndExpiresAtGreaterThanEqual(longUrl, now))
                .thenReturn(Optional.of(mapping));

        Optional<UrlMapping> result = urlService.fetchByLongUrl(longUrl, now);

        assertTrue(result.isPresent());
        assertEquals(longUrl, result.get().getLongUrl());
        verify(urlMappingRepository).findByLongUrlAndExpiresAtGreaterThanEqual(longUrl, now);
    }

    @Test
    void fetchByLongUrl_returnsEmpty_whenNotFound() {
        when(urlMappingRepository.findByLongUrlAndExpiresAtGreaterThanEqual(longUrl, now))
                .thenReturn(Optional.empty());

        Optional<UrlMapping> result = urlService.fetchByLongUrl(longUrl, now);

        assertFalse(result.isPresent());
        verify(urlMappingRepository).findByLongUrlAndExpiresAtGreaterThanEqual(longUrl, now);
    }

    @Test
    void createShortUrlCode_createsAndSavesNewMapping_whenCodeIsUnique() {
        ShortenUrlRequest request = new ShortenUrlRequest(longUrl);
        Instant expiresAt = now.plusSeconds(3600);

        try (MockedStatic<UrlHashUtil> mockedUtil = Mockito.mockStatic(UrlHashUtil.class)) {
            mockedUtil.when(() -> UrlHashUtil.generateMd5Hash(longUrl)).thenReturn(hash);
            mockedUtil.when(UrlHashUtil::calculateExpiresAt).thenReturn(expiresAt);

            when(urlMappingRepository.findByCodeAndExpiresAtGreaterThanEqual(hash, any(Instant.class)))
                    .thenReturn(Optional.empty());

            UrlMapping saved = new UrlMapping();
            saved.setCode(hash);
            saved.setLongUrl(longUrl);
            saved.setExpiresAt(expiresAt);
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(saved);

            UrlMapping result = urlService.createShortUrlCode(request);

            assertNotNull(result);
            assertEquals(hash, result.getCode());
            assertEquals(longUrl, result.getLongUrl());
            assertEquals(expiresAt, result.getExpiresAt());

            verify(urlMappingRepository).findByCodeAndExpiresAtGreaterThanEqual(eq(hash), any(Instant.class));
            verify(urlMappingRepository).save(any(UrlMapping.class));
        }
    }

    @Test
    void createShortUrlCode_retriesOnCollision() {
        ShortenUrlRequest request = new ShortenUrlRequest(longUrl);
        Instant expiresAt = now.plusSeconds(3600);
        String hash1 = "collision";
        String hash2 = "unique";

        try (MockedStatic<UrlHashUtil> mockedUtil = Mockito.mockStatic(UrlHashUtil.class)) {
            // First call returns colliding hash, second returns unique
            mockedUtil.when(() -> UrlHashUtil.generateMd5Hash(longUrl))
                    .thenReturn(hash1)
                    .thenReturn(hash2);
            mockedUtil.when(UrlHashUtil::calculateExpiresAt).thenReturn(expiresAt);

            // First lookup: exists → collision
            UrlMapping existing = new UrlMapping();
            existing.setCode(hash1);
            when(urlMappingRepository.findByCodeAndExpiresAtGreaterThanEqual(hash1, any(Instant.class)))
                    .thenReturn(Optional.of(existing));

            // Second lookup: empty → available
            when(urlMappingRepository.findByCodeAndExpiresAtGreaterThanEqual(hash2, any(Instant.class)))
                    .thenReturn(Optional.empty());

            UrlMapping saved = new UrlMapping();
            saved.setCode(hash2);
            saved.setLongUrl(longUrl);
            saved.setExpiresAt(expiresAt);
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(saved);

            UrlMapping result = urlService.createShortUrlCode(request);

            assertEquals(hash2, result.getCode());
            verify(urlMappingRepository, times(2)).findByCodeAndExpiresAtGreaterThanEqual(anyString(), any(Instant.class));
            verify(urlMappingRepository).save(any(UrlMapping.class));
        }
    }

    @Test
    void updateHitCount_savesTheProvidedUrlMapping() {
        UrlMapping urlMap = new UrlMapping();
        urlMap.setCode("test");
        urlMap.setHitCount(10);

        urlService.updateHitCount(urlMap);

        verify(urlMappingRepository).save(urlMap);
    }
}