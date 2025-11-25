package ru.ssau.tk.pmi.filters;

import ru.ssau.tk.pmi.database.DatabaseConnection;
import ru.ssau.tk.pmi.repository.manual.UserDao;
import ru.ssau.tk.pmi.repository.manual.JdbcUserDao;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

public class BasicAuthFilter implements Filter {
    private static final Logger logger = Logger.getLogger(BasicAuthFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("BasicAuthFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Пропускаем OPTIONS запросы (CORS)
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Пропускаем публичные endpoints
        String path = httpRequest.getRequestURI();
        if (isPublicEndpoint(path, httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Проверяем аутентификацию
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            sendUnauthorized(httpResponse, "Missing or invalid Authorization header");
            return;
        }

        try {
            String base64Credentials = authHeader.substring("Basic ".length());
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            String[] values = credentials.split(":", 2);

            if (values.length != 2) {
                sendUnauthorized(httpResponse, "Invalid credentials format");
                return;
            }

            String username = values[0];
            String password = values[1];

            // Аутентификация пользователя
            Map<String, Object> user = authenticateUser(username, password);
            if (user == null) {
                sendUnauthorized(httpResponse, "Invalid username or password");
                return;
            }

            // Сохраняем пользователя в атрибуты запроса
            httpRequest.setAttribute("authenticatedUser", user);
            logger.info("User authenticated: " + username + " with role: " + user.get("role"));

            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.severe("Authentication error: " + e.getMessage());
            sendError(httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
        }
    }

    @Override
    public void destroy() {
        logger.info("BasicAuthFilter destroyed");
    }

    private boolean isPublicEndpoint(String path, String method) {
        // Публичные endpoints (не требуют аутентификации)
        return (path.matches(".*/users/register") && "POST".equalsIgnoreCase(method)) ||
                (path.matches(".*/users/login") && "POST".equalsIgnoreCase(method)) ||
                path.matches(".*/api-docs.*") ||
                path.matches(".*/swagger.*");
    }

    private Map<String, Object> authenticateUser(String username, String password) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);
            Map<String, Object> user = userDao.getUserByUsername(username);

            if (user != null) {
                String storedHash = (String) user.get("password_hash");
                // Простая проверка пароля
                if (storedHash.equals("hashed_" + password)) {
                    return user;
                }
            }
        } catch (Exception e) {
            logger.severe("Database error during authentication: " + e.getMessage());
        }
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"Math Functions API\"");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        logger.warning("Unauthorized access: " + message);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}