package com.maliroso.url_shortener.dto.response;

public record ShortenUrlResponse(
        String code,
        String shortUrl
) {
}
