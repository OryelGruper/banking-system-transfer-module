package com.bankingsystem.transfer.security;

import tools.jackson.databind.json.JsonMapper;
import com.bankingsystem.transfer.config.AppProperties;
import com.bankingsystem.transfer.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Requires a valid X-FIB-AUTH header on every /api/** call. Runs as a
 * plain servlet filter (before Spring MVC), so a rejection here writes
 * the error JSON itself instead of going through @RestControllerAdvice.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-FIB-AUTH";

    private final AppProperties properties;
    private final JsonMapper jsonMapper;

    public ApiKeyAuthFilter(AppProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health checks and other non-API paths skip auth.
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String suppliedKey = request.getHeader(HEADER);
        String expectedKey = properties.security().apiKey();

        if (suppliedKey == null || !suppliedKey.equals(expectedKey)) {
            writeUnauthorized(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Missing or invalid " + HEADER + " header",
                request.getRequestURI()
        );
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
