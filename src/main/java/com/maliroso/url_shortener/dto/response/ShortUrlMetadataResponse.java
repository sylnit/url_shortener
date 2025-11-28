package com.maliroso.url_shortener.dto.response;

import java.time.Instant;

public record ShortUrlMetadataResponse(
        String code,
        String shortUrl,
        String longUrl,
        Instant createdAt,
        long hitCount
) {
}
