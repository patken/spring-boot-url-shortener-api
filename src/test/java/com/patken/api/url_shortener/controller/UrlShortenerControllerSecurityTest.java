package com.patken.api.url_shortener.controller;

import com.patken.api.url_shortener.config.SecurityConfig;
import com.patken.api.url_shortener.service.UrlShortenerService;
import com.patken.api.url_shortener.util.UtilsForTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlShortenerController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.jwt.secret=test-secret-test-secret-test-secret-123")
class UrlShortenerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlShortenerService urlShortenerService;

    @Test
    @DisplayName("POST (write) is rejected with 401 when unauthenticated")
    void testCreateRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/url-shortener")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST (write) succeeds with 201 when a valid JWT is presented")
    void testCreateSucceedsWithJwt() throws Exception {
        when(urlShortenerService.addNewShortenUrl(any())).thenReturn(UtilsForTest.buildUrlShortenResponse());

        mockMvc.perform(post("/api/v1/url-shortener")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET (read) is public and does not require authentication")
    void testReadIsPublic() throws Exception {
        when(urlShortenerService.getOriginalUrl(any())).thenReturn(UtilsForTest.buildUrlShortenResponse());

        mockMvc.perform(get("/api/v1/url-shortener/{shortenUrl}", UtilsForTest.SHORTEN_URL))
                .andExpect(status().isOk());
    }
}
