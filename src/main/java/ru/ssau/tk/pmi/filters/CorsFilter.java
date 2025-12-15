package ru.ssau.tk.pmi.filters;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class CorsFilter implements Filter {

    private static final Logger logger = Logger.getLogger(CorsFilter.class.getName());
    private static final Set<String> ALLOWED_ORIGINS = new HashSet<>();
    private static final Set<String> ALLOWED_METHODS = new HashSet<>();

    static {
        // Разрешенные источники (можно добавить больше при необходимости)
        ALLOWED_ORIGINS.addAll(Arrays.asList(
                "http://localhost:5173",  // Vite dev server
                "http://127.0.0.1:5173",
                "http://localhost:3000",  // Create React App
                "http://127.0.0.1:3000",
                "http://localhost:8080",  // Сам Tomcat (на всякий случай)
                "http://127.0.0.1:8080"
        ));

        // Разрешенные HTTP методы
        ALLOWED_METHODS.addAll(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CORS Filter initialized");
        logger.info("Allowed origins: " + ALLOWED_ORIGINS);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String origin = request.getHeader("Origin");
        String requestMethod = request.getMethod();

        // Логируем запрос для отладки
        logger.info(String.format("CORS Request: %s %s (Origin: %s)",
                requestMethod, request.getRequestURI(), origin));

        // Проверяем Origin
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            logger.info("Allowed origin: " + origin);
        } else if (origin != null) {
            // Если Origin указан, но не в списке разрешенных
            logger.warning("Origin not allowed: " + origin);
        }

        // Устанавливаем обязательные CORS заголовки
        response.setHeader("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
        response.setHeader("Access-Control-Allow-Headers",
                "Authorization, Content-Type, Accept, X-Requested-With, Cache-Control, Origin, " +
                        "Access-Control-Request-Method, Access-Control-Request-Headers, X-API-Key");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600"); // 1 час кэширования preflight

        // Дополнительные полезные заголовки
        response.setHeader("Access-Control-Expose-Headers",
                "Authorization, Content-Type, X-Total-Count, X-Error-Message");

        // Обработка preflight OPTIONS запроса
        if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
            logger.info("Handling OPTIONS preflight request");
            response.setStatus(HttpServletResponse.SC_OK);

            // Добавляем заголовки для preflight
            if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
            }

            String requestedMethods = request.getHeader("Access-Control-Request-Method");
            if (requestedMethods != null) {
                response.setHeader("Access-Control-Allow-Methods", requestedMethods);
            }

            String requestedHeaders = request.getHeader("Access-Control-Request-Headers");
            if (requestedHeaders != null) {
                response.setHeader("Access-Control-Allow-Headers", requestedHeaders);
            }

            return; // Не продолжаем цепочку фильтров для OPTIONS
        }

        // Продолжаем цепочку фильтров для обычных запросов
        chain.doFilter(request, response);

        // Логируем успешное завершение
        logger.info(String.format("CORS Response: %s %s -> %d",
                requestMethod, request.getRequestURI(), response.getStatus()));
    }

    @Override
    public void destroy() {
        logger.info("CORS Filter destroyed");
        ALLOWED_ORIGINS.clear();
        ALLOWED_METHODS.clear();
    }
}