package ru.ssau.tk.pmi.service;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Разрешаем запросы с фронтенда (React/Vite)
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",  // Vite dev server
                "http://localhost:3000",  // Create React App
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));

        // Разрешаем методы
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Разрешаем заголовки
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Разрешаем куки/авторизацию
        config.setAllowCredentials(true);

        // Время кэширования CORS preflight
        config.setMaxAge(3600L);

        // Добавляем CORS для всех эндпоинтов
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/**", config); // На всякий случай

        return new CorsFilter(source);
    }
}
