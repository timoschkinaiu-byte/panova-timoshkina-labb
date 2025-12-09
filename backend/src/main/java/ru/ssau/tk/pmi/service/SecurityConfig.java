package ru.ssau.tk.pmi.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/users/register",
            "/api/users/login",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
    };

    private static final String[] USER_ENDPOINTS = {
            "/api/functions/**",
            "/api/points/**",
            "/api/operations/**",
            "/api/access/**"
    };

    private static final String[] ADMIN_ENDPOINTS = {
            "/api/users/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Отключаем CSRF для API
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless для REST API
                .authorizeHttpRequests(authz -> authz
                        // Публичные endpoints
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Admin endpoints
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")

                        // User endpoints
                        .requestMatchers(USER_ENDPOINTS).hasAnyRole("USER", "ADMIN")

                        // Все остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic ->
                        httpBasic.authenticationEntryPoint(basicAuthenticationEntryPoint())
                );

        return http.build();
    }

    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("Math Functions API");
        return entryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Для хеширования паролей
    }

}
