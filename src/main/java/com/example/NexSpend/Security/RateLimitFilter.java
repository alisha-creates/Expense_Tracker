package com.example.NexSpend.Security;

import com.example.NexSpend.Service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String ip =
                request.getRemoteAddr();

        Bucket bucket =
                rateLimitService.resolveBucket(ip);

        if (bucket.tryConsume(1)) {

            filterChain.doFilter(
                    request,
                    response
            );

        } else {

            response.setStatus(429);

            response.getWriter()
                    .write(
                            "Too many requests"
                    );
        }
    }
}
