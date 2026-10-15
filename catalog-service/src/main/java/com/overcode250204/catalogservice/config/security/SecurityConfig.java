package com.overcode250204.catalogservice.config.security;

import com.overcode250204.common.security.InternalGatewayAuthenticationFilter;
import com.overcode250204.common.web.RequestLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Import({InternalGatewayAuthenticationFilter.class, RequestLoggingFilter.class})
public class SecurityConfig {
    private final InternalGatewayAuthenticationFilter internalGatewayFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RequestLoggingFilter requestLoggingFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalGatewayFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
