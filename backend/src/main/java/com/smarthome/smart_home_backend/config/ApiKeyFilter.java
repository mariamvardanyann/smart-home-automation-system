package com.smarthome.smart_home_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Deliberately simple single-key access gate for local demos, not user authentication. */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final byte[] expected;
    public ApiKeyFilter(@Value("${smart-home.api-key:}") String key) {
        if (key == null || key.isBlank() || key.length() < 16)
            throw new IllegalStateException("Set SMART_HOME_API_KEY (at least 16 characters) before starting");
        this.expected = key.getBytes(StandardCharsets.UTF_8);
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || "/hello".equals(request.getServletPath());
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader("X-API-Key");
        if (provided == null || !MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"API key required\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
