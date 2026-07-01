package com.patken.api.url_shortener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Swagger UI and the served contract are publicly reachable, and the UI is wired to render
 */
@SpringBootTest(properties = {
        "DATASOURCE_URL=jdbc:h2:mem:swagger-ui;DB_CLOSE_DELAY=-1",
        "DDL_AUTO=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SwaggerUiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger UI is served publicly")
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("The hand-written OpenAPI contract is served publicly")
    void contractIsServed() throws Exception {
        mockMvc.perform(get("/openapi/oas3.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Url-Shortener-Endpoint")));
    }

    @Test
    @DisplayName("Swagger config points the UI at the static contract")
    void swaggerConfigUsesTheContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/openapi/oas3.yaml"));
    }
}
