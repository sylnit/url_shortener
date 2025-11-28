package com.maliroso.url_shortener.repository;

import com.maliroso.url_shortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByCodeAndExpiresAtGreaterThanEqual(String code, Instant currentDateTime);

    Optional<UrlMapping> findByLongUrlAndExpiresAtGreaterThanEqual(String longUrl, Instant currentDateTime);
}
