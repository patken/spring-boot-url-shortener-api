package com.patken.api.url_shortener.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Getter
@Setter
@ToString
@SuperBuilder
@RequiredArgsConstructor
@Entity
@Table(name = "url")
public class UrlEntity extends CommonEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "url_id")
    private Integer urlId;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "shorten_url", nullable = false, length = 10)
    private String shortenUrl;
}
