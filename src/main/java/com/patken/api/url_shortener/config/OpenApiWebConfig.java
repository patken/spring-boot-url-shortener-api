package com.patken.api.url_shortener.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the hand-written OpenAPI contract ({@code src/main/resources/openapi/oas3.yaml})
 * over HTTP so Swagger UI can render the authoritative, contract-first definition rather
 * than one re-generated from the code.
 */
@Configuration
public class OpenApiWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/openapi/**")
                .addResourceLocations("classpath:/openapi/");
    }
}
