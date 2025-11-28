package com.maliroso.url_shortener.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectMetricsServiceTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private RedirectMetricsService redirectMetricsService;

    @BeforeEach
    void setUp() {
        // Stub the MeterRegistry to return our mocked Counter when Counter.builder(...).register() is called
        when(meterRegistry.counter(any(), any(), any()))
                .thenReturn(counter);

        redirectMetricsService = new RedirectMetricsService(meterRegistry);
    }

    @Test
    void recordRedirect_shouldIncrementCounter() {
        // When
        redirectMetricsService.recordRedirect();

        // Then
        verify(counter).increment();
        verify(meterRegistry).counter(eq("shortener_redirect_total"), any(), any());
    }
}