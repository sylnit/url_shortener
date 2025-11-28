package com.maliroso.url_shortener.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record ShortenUrlRequest(
        @NotBlank(message = "Long URL cannot be blank")
        @URL(message = "Invalid URL format")
        String longUrl
) {
}
