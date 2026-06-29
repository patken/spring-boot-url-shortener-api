package com.patken.api.url_shortener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patken.api.url_shortener.dto.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration test against a real PostgreSQL (Testcontainers).
 * <p>
 * Proves the Flyway migrations (portable DDL) apply on PostgreSQL — not only H2 —
 * with {@code ddl-auto=validate}, and exercises the whole HTTP flow end to end:
 * register, login, create (authenticated), resolve, list and deduplication.
 * <p>
 * Skipped automatically when no Docker daemon is available (e.g. local dev without
 * Docker); runs in CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Testcontainers(disabledWithoutDocker = true)
class UrlShortenerPostgresIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("DRIVER_CLASS_NAME", () -> "org.postgresql.Driver");
        registry.add("DATABASE_PLATFORM", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.h2.console.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CREDENTIALS = "{\"username\":\"carol\",\"password\":\"password123\"}";
    private static final String ORIGINAL_URL = "https://www.postgres-example.com/a/long/path";
    private static final String CREATE_BODY = "{\"url\":\"" + ORIGINAL_URL + "\"}";

    @Test
    @DisplayName("register -> login -> create -> resolve -> list -> dedup on PostgreSQL")
    void endToEndFlowOnPostgres() throws Exception {
        // register + login
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isCreated());
        var loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var token = objectMapper.readValue(loginBody, TokenResponse.class).accessToken();
        var bearer = "Bearer " + token;

        // create (authenticated)
        var createBody = mockMvc.perform(post("/api/v1/url-shortener")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortenUrl").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl").value(ORIGINAL_URL))
                .andReturn().getResponse().getContentAsString();
        var shortenUrl = objectMapper.readTree(createBody).get("shortenUrl").asText();

        // resolve (public)
        mockMvc.perform(get("/api/v1/url-shortener/{shortenUrl}", shortenUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value(ORIGINAL_URL));

        // list (public)
        mockMvc.perform(get("/api/v1/url-shortener").param("page", "0").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.records[0].shortenUrl").value(shortenUrl));

        // deduplication: same original url returns the same short key
        var secondCreate = mockMvc.perform(post("/api/v1/url-shortener")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertEquals(shortenUrl, objectMapper.readTree(secondCreate).get("shortenUrl").asText());
    }
}
