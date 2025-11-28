package com.maliroso.url_shortener.service;

import com.maliroso.url_shortener.dto.request.ShortenUrlRequest;
import com.maliroso.url_shortener.model.UrlMapping;
import com.maliroso.url_shortener.repository.UrlMappingRepository;
import com.maliroso.url_shortener.utils.UrlHashUtil;
import jdk.jshell.execution.Util;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UrlService {
    @Autowired
    private UrlMappingRepository urlMappingRepository;

    public Optional<UrlMapping> fetchUrlMapping(String code, Instant instant){
        return urlMappingRepository.findByCodeAndExpiresAtGreaterThanEqual(code, instant);
    }

    public Optional<UrlMapping> fetchByLongUrl(String longUrl, Instant instant){
        return urlMappingRepository.findByLongUrlAndExpiresAtGreaterThanEqual(longUrl, instant);
    }

    public UrlMapping createShortUrlCode(ShortenUrlRequest request){
        boolean isValidHash = false;
        String code = "";
        while(! isValidHash){
            code = UrlHashUtil.generateMd5Hash(request.longUrl());
            Instant instant = Instant.now();
            if(fetchUrlMapping(code, instant).isEmpty()){
                isValidHash = true;
            }
        }

        UrlMapping newUrlMapping = new UrlMapping();
        newUrlMapping.setCode(code);
        newUrlMapping.setLongUrl(request.longUrl());
        newUrlMapping.setExpiresAt(UrlHashUtil.calculateExpiresAt());

        return urlMappingRepository.save(newUrlMapping);
    }

    public void updateHitCount(UrlMapping urlMap) {
        urlMappingRepository.save(urlMap);
    }
}
