package com.maliroso.url_shortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "url_mappings", indexes = {
        @Index(name = "idx_code", columnList = "code")
})
public class UrlMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String longUrl;

    private long hitCount;

    @CreationTimestamp
    private Instant createdAt;

    private Instant expiresAt;
}
