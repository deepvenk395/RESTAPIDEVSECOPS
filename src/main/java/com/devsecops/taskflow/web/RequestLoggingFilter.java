package com.devsecops.taskflow.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (request.getDispatcherType() != jakarta.servlet.DispatcherType.REQUEST) {
            chain.doFilter(request, response);
            return;
        }

        String requestId = uuid();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.debug("requestId={} authorization={}", requestId, authorization);

        long start = System.nanoTime();
        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapped);
        } finally {
            long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            log.info("method={} path={} query={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    wrapped.getStatus(),
                    durationMs);
            wrapped.copyBodyToResponse();
            MDC.remove("requestId");
        }
    }

    private String uuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}