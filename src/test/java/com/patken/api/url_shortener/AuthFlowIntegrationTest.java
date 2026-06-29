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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end happy path of the in-app authentication, exercising the real
 * security chain
 */
@SpringBootTest(properties = {
        "DATASOURCE_URL=jdbc:h2:mem:auth-flow;DB_CLOSE_DELAY=-1",
        "DDL_AUTO=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CREDENTIALS = "{\"username\":\"alice\",\"password\":\"password123\"}";
    private static final String CREATE_BODY = "{\"url\":\"https://www.example.com/some/long/path\"}";

    @Test
    @DisplayName("register -> login -> create a short url with the issued token")
    void fullAuthenticatedFlow() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isCreated());

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        var token = objectMapper
                .readValue(loginResult.getResponse().getContentAsString(), TokenResponse.class)
                .accessToken();

        mockMvc.perform(post("/api/v1/url-shortener")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/url-shortener")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortenUrl").isNotEmpty())
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com/some/long/path"));
    }

    @Test
    @DisplayName("register twice with the same username -> 409 conflict")
    void duplicateRegistration() throws Exception {
        var creds = "{\"username\":\"bob\",\"password\":\"password123\"}";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isConflict());
    }
}
