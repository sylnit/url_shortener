package com.maliroso.url_shortener.filter;

import com.maliroso.url_shortener.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private io.github.bucket4j.Bucket bucket;

    @Mock
    private PrintWriter printWriter;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() throws IOException {
        rateLimitFilter = new RateLimitFilter();
        // Use reflection to inject the mocked service (since @Autowired isn't processed)
        try {
            java.lang.reflect.Field field = RateLimitFilter.class.getDeclaredField("rateLimiterService");
            field.setAccessible(true);
            field.set(rateLimitFilter, rateLimiterService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject rateLimiterService", e);
        }

        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void doFilter_allowsRequest_whenTokenAvailable() throws ServletException, IOException {
        // Given
        String clientIp = "192.168.1.100";
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(rateLimiterService.resolveBucket(eq(clientIp), eq(10L), eq(java.time.Duration.ofMinutes(1))))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        // When
        rateLimitFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        verify(printWriter, never()).write(anyString());
    }

    @Test
    void doFilter_returns429_whenRateLimitExceeded() throws ServletException, IOException {
        // Given
        String clientIp = "10.0.0.5";
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(rateLimiterService.resolveBucket(eq(clientIp), eq(10L), eq(java.time.Duration.ofMinutes(1))))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        // When
        rateLimitFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).setStatus(429);
        verify(printWriter).write("Rate limit exceeded. Try again later.");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void getClientIp_usesXForwardedForHeader_whenPresent() throws ServletException, IOException {
        // Given
        String forwardedIp = "203.0.113.42";
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedIp);
        when(rateLimiterService.resolveBucket(eq(forwardedIp), anyLong(), any(java.time.Duration.class)))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        // When
        rateLimitFilter.doFilter(request, response, filterChain);

        // Then
        verify(rateLimiterService).resolveBucket(eq(forwardedIp), eq(10L), eq(java.time.Duration.ofMinutes(1)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void getClientIp_fallsBackToRemoteAddr_whenXForwardedForIsInvalid() throws ServletException, IOException {
        // Given
        String remoteAddr = "192.0.2.1";
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(rateLimiterService.resolveBucket(eq(remoteAddr), anyLong(), any(java.time.Duration.class)))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        // When
        rateLimitFilter.doFilter(request, response, filterChain);

        // Then
        verify(rateLimiterService).resolveBucket(eq(remoteAddr), eq(10L), eq(java.time.Duration.ofMinutes(1)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void getClientIp_ignoresEmptyXForwardedFor() throws ServletException, IOException {
        // Given
        String remoteAddr = "198.51.100.7";
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(rateLimiterService.resolveBucket(eq(remoteAddr), anyLong(), any(java.time.Duration.class)))
                .thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        // When
        rateLimitFilter.doFilter(request, response, filterChain);

        // Then
        verify(rateLimiterService).resolveBucket(eq(remoteAddr), eq(10L), eq(java.time.Duration.ofMinutes(1)));
        verify(filterChain).doFilter(request, response);
    }
}