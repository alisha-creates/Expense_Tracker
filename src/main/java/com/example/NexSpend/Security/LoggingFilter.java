package com.example.NexSpend.Security;

import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request,1024);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String requestBody = getStringFromByteArray(requestWrapper.getContentAsByteArray());
            String responseBody = getStringFromByteArray(responseWrapper.getContentAsByteArray());

            log.info("=== API Request ===");
            log.info("Time: {}", LocalDateTime.now());
            log.info("Method: {} {}", requestWrapper.getMethod(), requestWrapper.getRequestURI());
            log.info("User: {}", requestWrapper.getUserPrincipal() != null ? requestWrapper.getUserPrincipal().getName() : "Anonymous");
            log.info("Request Body: {}", requestBody.isEmpty() ? "[EMPTY]" : requestBody);
            log.info("Status: {} | Duration: {}ms", responseWrapper.getStatus(), duration);
            log.info("Response Body: {}", responseBody.length() > 500
                    ? responseBody.substring(0, 500) + "..."
                    : responseBody);
            log.info("=== End Request ===\n");

            responseWrapper.copyBodyToResponse();
        }
    }

    private String getStringFromByteArray(byte[] bytes) {
        return bytes.length > 0 ? new String(bytes, StandardCharsets.UTF_8) : "";
    }
}
