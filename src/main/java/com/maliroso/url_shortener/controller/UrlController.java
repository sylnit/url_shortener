package com.maliroso.url_shortener.controller;

import com.maliroso.url_shortener.dto.request.ShortenUrlRequest;
import com.maliroso.url_shortener.dto.response.ShortenUrlResponse;
import com.maliroso.url_shortener.model.UrlMapping;
import com.maliroso.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
public class UrlController {
    @Autowired
    private UrlService urlService;

    @Value("${base_url")
    private String baseUrl;

    @Transactional
    @PostMapping("/api/urls")
    public ResponseEntity<?> createShortUrl(
            @Valid @RequestBody ShortenUrlRequest request
    ){
        try {
            Optional<UrlMapping> urlMapSearch = urlService.fetchByLongUrl(request.longUrl(), Instant.now());
            if(urlMapSearch.isPresent()){
                UrlMapping urlMap = urlMapSearch.get();
                String shortUrl = baseUrl + "r/" + urlMap.getCode();
                ShortenUrlResponse urlResponse = new ShortenUrlResponse(urlMap.getCode(), shortUrl);
                return ResponseEntity.status(HttpStatus.OK).body(urlResponse);
            }

            UrlMapping urlMap = urlService.createShortUrlCode(request);


            String shortUrl = baseUrl + "r/" + urlMap.getCode();
            ShortenUrlResponse urlResponse = new ShortenUrlResponse(urlMap.getCode(), shortUrl);
            return ResponseEntity.status(HttpStatus.OK).body(urlResponse);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(e.getMessage());
        }
    }

    @Transactional
    @GetMapping("/api/urls/{code}")
    public ResponseEntity<?> getUrlMetadata(
            @PathVariable(name = "code") String code
    ){
        try {
            Optional<UrlMapping> urlMapSearch = urlService.fetchByLongUrl(code, Instant.now());
            if(urlMapSearch.isPresent()){
                UrlMapping urlMap = urlMapSearch.get();
                String shortUrl = baseUrl + "r/" + urlMap.getCode();
                ShortenUrlResponse urlResponse = new ShortenUrlResponse(urlMap.getCode(), shortUrl);

                urlMap.setHitCount(urlMap.getHitCount() + 1);
                urlService.updateHitCount(urlMap);

                return ResponseEntity.status(HttpStatus.OK).body(urlResponse);
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(e.getMessage());
        }
    }

    @Transactional
    @GetMapping("/r/{code}")
    public ResponseEntity<?> redirectToUrl(
            @PathVariable(name = "code") String code
    ){
        try{
            Optional<UrlMapping> urlMapSearch = urlService.fetchUrlMapping(code, Instant.now());
            if(urlMapSearch.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            UrlMapping urlMap = urlMapSearch.get();

            urlMap.setHitCount(urlMap.getHitCount() + 1);
            urlService.updateHitCount(urlMap);

            String longUrl = urlMap.getLongUrl();

            if(longUrl != null && ! longUrl.startsWith("http")){
                longUrl = "http://"+longUrl;
            }

            HttpHeaders headers = new HttpHeaders();
            if (longUrl != null) {
                headers.setLocation(URI.create(longUrl));
            }
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
