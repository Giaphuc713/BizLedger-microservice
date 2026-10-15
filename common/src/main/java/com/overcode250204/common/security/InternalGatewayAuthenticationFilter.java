package com.overcode250204.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class InternalGatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String tenantId = request.getHeader("X-Tenant-Id");
        String rolesStr = request.getHeader("X-Roles");
        String permsStr = request.getHeader("X-Permissions");

        String path = request.getRequestURI();

        if (isPublicPath(path)
        ) {
            filterChain.doFilter(request, response);
            return;
        }


        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if (rolesStr != null && !rolesStr.isBlank()) {
                for (String role : rolesStr.trim().split(",")) {
                    if (!role.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
                    }
                }
            }

            if (permsStr != null && !permsStr.isBlank()) {
                for (String perm : permsStr.trim().split(",")) {
                    if (!perm.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(perm.trim()));
                    }
                }
            }

            // Create Authenticated Principle details
            InternalUser principal = new InternalUser(userId, tenantId, rolesStr, permsStr);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
                    null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    public record InternalUser(String userId, String tenantId, String roles, String permissions) {
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars") || path.startsWith("/api/v1/auth") || path.startsWith("/actuator");
    }
}
