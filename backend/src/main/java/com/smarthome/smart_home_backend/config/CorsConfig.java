package com.smarthome.smart_home_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${smart-home.cors.allowed-origins:http://localhost:5173}") String origins) {
        String[] allowed = java.util.Arrays.stream(origins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(allowed)
                        .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                        .allowedHeaders("X-API-Key", "Content-Type");
            }
        };
    }
}
