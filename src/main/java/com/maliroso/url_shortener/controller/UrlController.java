package com.maliroso.url_shortener.controller;

import com.maliroso.url_shortener.dto.request.ShortenUrlRequest;
import com.maliroso.url_shortener.dto.response.ShortUrlMetadataResponse;
import com.maliroso.url_shortener.dto.response.ShortenUrlResponse;
import com.maliroso.url_shortener.model.UrlMapping;
import com.maliroso.url_shortener.service.RedirectMetricsService;
import com.maliroso.url_shortener.service.UrlService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Autowired
    private RedirectMetricsService metricsService;

    private final String baseUrl = "http://localhost:8080/";

    @Operation(summary = "Create a short url for a long url")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Short url created for long url",
                content = @Content(schema = @Schema(implementation = ShortenUrlResponse.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable content")
    })
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

    @Operation(summary = "Get metadata for url code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata for url code retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ShortUrlMetadataResponse.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable content")
    })
    @Transactional
    @GetMapping("/api/urls/{code}")
    public ResponseEntity<?> getUrlMetadata(
            @PathVariable(name = "code") String code
    ){
        try {
            Optional<UrlMapping> urlMapSearch = urlService.fetchUrlMapping(code, Instant.now());
            if(urlMapSearch.isPresent()){
                UrlMapping urlMap = urlMapSearch.get();
                String shortUrl = baseUrl + "r/" + urlMap.getCode();
                ShortUrlMetadataResponse urlResponse = new ShortUrlMetadataResponse(
                        urlMap.getCode(),
                        shortUrl,
                        urlMap.getLongUrl(),
                        urlMap.getCreatedAt(),
                        urlMap.getHitCount()
                );

                urlMap.setHitCount(urlMap.getHitCount() + 1);
                urlService.updateHitCount(urlMap);

                return ResponseEntity.status(HttpStatus.OK).body(urlResponse);
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(e.getMessage());
        }
    }

    @Operation(summary = "Redirect to long url from short url")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirected to log url"),
            @ApiResponse(responseCode = "404", description = "Url Not Found")
    })
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

            //record redirect for actuator endpoints
            metricsService.recordRedirect();

            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
