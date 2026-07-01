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
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Token lifecycle: refresh rotation, refresh-as-access rejection, and logout revocation.
 * Exercises the real JWT round trip and the Hazelcast-backed revocation state.
 */
@SpringBootTest(properties = {
        "DATASOURCE_URL=jdbc:h2:mem:token-lifecycle;DB_CLOSE_DELAY=-1",
        "DDL_AUTO=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TokenLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String CREDENTIALS = "{\"username\":\"dave\",\"password\":\"password123\"}";
    private static final String CREATE_BODY = "{\"url\":\"https://www.example.com/token-lifecycle\"}";

    @Test
    @DisplayName("refresh rotates tokens; refresh-as-access is rejected; logout revokes the refresh token")
    void tokenLifecycle() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS)).andExpect(status().isCreated());

        var first = login();

        // the access token works on the protected endpoint
        create(first.accessToken()).andExpect(status().isCreated());

        // refresh -> new tokens
        var refreshed = refresh(first.refreshToken());
        create(refreshed.accessToken()).andExpect(status().isCreated());

        // a refresh token must not be usable as a Bearer access token
        create(refreshed.refreshToken()).andExpect(status().isUnauthorized());

        // logout revokes the refresh token: refreshing with it now fails
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshed.accessToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshed.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private TokenResponse login() throws Exception {
        var body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, TokenResponse.class);
    }

    private TokenResponse refresh(String refreshToken) throws Exception {
        var body = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, TokenResponse.class);
    }

    private org.springframework.test.web.servlet.ResultActions create(String bearerToken) throws Exception {
        return mockMvc.perform(post("/api/v1/url-shortener")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY));
    }
}
