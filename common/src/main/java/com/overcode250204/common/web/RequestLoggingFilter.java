package com.overcode250204.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String method = request.getMethod();

        String traceId = request.getHeader("X-Trace-Id");
        String userId = request.getHeader("X-User-Id");
        String tenantId = request.getHeader("X-Tenant-Id");

        // Put to MDC for log tracing logback context integration
        if (traceId != null && !traceId.isBlank()) {
            MDC.put("traceId", traceId);
        }
        if (userId != null && !userId.isBlank()) {
            MDC.put("userId", userId);
        }
        if (tenantId != null && !tenantId.isBlank()) {
            MDC.put("tenantId", tenantId);
        }

        log.info("[Request Start] {} {} | traceId={} userId={} tenantId={}",
                method, path,
                traceId != null ? traceId : "none",
                userId != null ? userId : "anonymous",
                tenantId != null ? tenantId : "none");

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            log.info("[Request End] {} {} | status={} duration={}ms",
                    method, path, status, duration);
            MDC.clear();
        }
    }
}
