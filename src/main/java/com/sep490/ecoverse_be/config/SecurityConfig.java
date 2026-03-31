package com.sep490.ecoverse_be.config;

import com.sep490.ecoverse_be.service.IAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    @Lazy
    private IAuthenticationService authenticationService;

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(AppConstants.PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // --- Config cho WebSocket / SockJS endpoints ---
        CorsConfiguration wsConfig = new CorsConfiguration();
        wsConfig.setAllowedOriginPatterns(List.of("*"));
        wsConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        wsConfig.setAllowedHeaders(List.of("*"));
        wsConfig.setAllowCredentials(true);
        wsConfig.setMaxAge(3600L);

        // --- Config cho REST API endpoints ---
        CorsConfiguration apiConfig = new CorsConfiguration();
        apiConfig.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8081",
                "https://api.ecoverse-system.io.vn",
                "https://www.api.ecoverse-system.io.vn",
                "https://ecoverse-system.io.vn",
                "https://www.ecoverse-system.io.vn"
        ));
        apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        apiConfig.setAllowedHeaders(List.of("*"));
        apiConfig.setExposedHeaders(List.of(
                "X-Mode", "X-Center", "X-RadiusKm", "X-Limit", "X-Count", "X-Has-More",
                "X-BBox", "X-District", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset"
        ));
        apiConfig.setAllowCredentials(true);
        apiConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // WS config phai dang ky truoc vi UrlBasedCorsConfigurationSource khop pattern dau tien
        source.registerCorsConfiguration("/ws/**", wsConfig);
        source.registerCorsConfiguration("/**", apiConfig);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(authenticationService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
