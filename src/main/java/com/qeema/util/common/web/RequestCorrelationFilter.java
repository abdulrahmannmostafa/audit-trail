package com.qeema.util.common.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = correlationId(request.getHeader(HEADER_NAME));
        response.setHeader(HEADER_NAME, correlationId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(HEADER_NAME, correlationId)) {
            filterChain.doFilter(request, response);
        }
    }

    private String correlationId(String requestedId) {
        if (requestedId != null && SAFE_CORRELATION_ID.matcher(requestedId).matches()) {
            return requestedId;
        }
        return UUID.randomUUID().toString();
    }
}
