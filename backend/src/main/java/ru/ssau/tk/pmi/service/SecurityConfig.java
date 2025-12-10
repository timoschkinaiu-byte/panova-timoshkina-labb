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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // 1. Публичные endpoints
                        .requestMatchers("/api/users/register").permitAll()  // Регистрация публичная
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()

                        // 2. /api/users/me для всех авторизованных
                        .requestMatchers("/api/users/me").authenticated()

                        // 3. Admin endpoints (операции над другими пользователями)
                        .requestMatchers("/api/users/{id}/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/role/**").hasRole("ADMIN")

                        // 4. User endpoints
                        .requestMatchers("/api/functions/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/points/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/operations/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/access/**").hasAnyRole("USER", "ADMIN")

                        // 5. ВСЕ остальные /api/users/** - только ADMIN
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // 6. Все остальные запросы
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
