package com.overcode250204.identityservice.filter;

import com.overcode250204.identityservice.entity.AuthenticatedUser;
import com.overcode250204.identityservice.service.impl.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        String path = request.getRequestURI();

        if (path.endsWith("/login")
                || isPublicPath(path)
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }



        String token = authorization.substring("Bearer ".length());

        try {
            AuthenticatedUser user = jwtTokenService.parseToken(token);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            for (String role : user.roles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            for (String permission : user.permissions()) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
    private boolean isPublicPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars") || path.startsWith("/api/v1/auth");
    }
}
