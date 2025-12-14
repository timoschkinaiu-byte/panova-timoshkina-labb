package ru.ssau.tk.pmi.filters;

import ru.ssau.tk.pmi.database.DatabaseConnection;
import ru.ssau.tk.pmi.repository.manual.UserDao;
import ru.ssau.tk.pmi.repository.manual.JdbcUserDao;
import org.mindrot.jbcrypt.BCrypt;

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

        // === ДОБАВЛЕННЫЙ ЛОГ ===
        logger.info("=== FILTER DEBUG ===");
        logger.info("URL: " + httpRequest.getRequestURL());
        logger.info("Method: " + httpRequest.getMethod());
        logger.info("Path: " + httpRequest.getRequestURI());
        // ======================

        // Пропускаем OPTIONS запросы (CORS)
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            logger.info("Skipping OPTIONS request");
            chain.doFilter(request, response);
            return;
        }

        // Пропускаем публичные endpoints
        String path = httpRequest.getRequestURI();
        logger.info("Checking if path is public: " + path);
        if (isPublicEndpoint(path, httpRequest.getMethod())) {
            logger.info("Path is public, allowing access");
            chain.doFilter(request, response);
            return;
        }

        // Проверяем аутентификацию
        String authHeader = httpRequest.getHeader("Authorization");
        logger.info("Authorization header: " + (authHeader != null ? authHeader.substring(0, Math.min(authHeader.length(), 50)) + "..." : "NULL"));

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            logger.warning("Missing or invalid Authorization header");
            sendUnauthorized(httpResponse, "Missing or invalid Authorization header");
            return;
        }

        try {
            String base64Credentials = authHeader.substring("Basic ".length());
            logger.info("Base64 credentials: " + base64Credentials);

            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            logger.info("Decoded credentials: " + credentials);

            String[] values = credentials.split(":", 2);
            logger.info("Split into " + values.length + " parts");

            if (values.length != 2) {
                logger.warning("Invalid credentials format, expected 2 parts");
                sendUnauthorized(httpResponse, "Invalid credentials format");
                return;
            }

            String username = values[0];
            String password = values[1];

            // === ДОБАВЛЕННЫЙ ЛОГ ===
            logger.info("=== ATTEMPTING AUTH ===");
            logger.info("Username: " + username);
            logger.info("Password length: " + password.length());
            // ======================

            // Аутентификация пользователя
            Map<String, Object> user = authenticateUser(username, password);
            if (user == null) {
                logger.warning("Authentication failed for user: " + username);
                sendUnauthorized(httpResponse, "Invalid username or password");
                return;
            }

            // Сохраняем пользователя в атрибуты запроса
            httpRequest.setAttribute("authenticatedUser", user);
            logger.info("✓ User authenticated: " + username + " with role: " + user.get("role"));

            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.severe("Authentication error: " + e.getMessage());
            e.printStackTrace();
            sendError(httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
        }
    }

    @Override
    public void destroy() {
        logger.info("BasicAuthFilter destroyed");
    }

    private boolean isPublicEndpoint(String path, String method) {
        boolean isPublic = (path.matches(".*/api/users/register") && "POST".equalsIgnoreCase(method)) ||
                (path.matches(".*/api/users/login") && "POST".equalsIgnoreCase(method)) ||
                path.matches(".*/api-docs.*") ||
                path.matches(".*/swagger.*");

        logger.info("isPublicEndpoint check: path=" + path + ", method=" + method + " -> " + isPublic);
        return isPublic;
    }

    private Map<String, Object> authenticateUser(String username, String password) {
        logger.info("=== AUTHENTICATE USER DEBUG ===");
        logger.info("Looking for user: " + username);

        try (Connection connection = DatabaseConnection.getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);
            Map<String, Object> user = userDao.getUserByUsername(username);

            if (user == null) {
                logger.warning("❌ User not found in database: " + username);
                return null;
            }

            logger.info("✅ User found in DB");
            logger.info("User ID: " + user.get("user_id"));
            logger.info("Username: " + user.get("username"));
            logger.info("Role: " + user.get("role"));
            logger.info("Enabled: " + user.get("enabled"));

            String storedHash = (String) user.get("password_hash");
            Boolean enabled = (Boolean) user.get("enabled");

            if (storedHash == null) {
                logger.warning("❌ No password hash stored for user: " + username);
                return null;
            }

            logger.info("Stored hash length: " + storedHash.length());
            logger.info("Stored hash start: " + storedHash.substring(0, Math.min(30, storedHash.length())) + "...");

            // Проверяем, активен ли пользователь
            if (enabled != null && !enabled) {
                logger.warning("❌ User account is disabled: " + username);
                return null;
            }

            // ПРОВЕРКА BCrypt
            logger.info("Checking BCrypt password...");
            boolean passwordMatches = BCrypt.checkpw(password, storedHash);
            logger.info("BCrypt check result: " + passwordMatches);

            if (passwordMatches) {
                logger.info("✅ Password matches!");
                return user;
            } else {
                logger.warning("❌ Password does not match!");
                logger.info("Provided password: " + password);

                // Для отладки: попробуем создать новый хеш с тем же паролем
                String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
                logger.info("New hash for comparison: " + newHash);
            }

            // Совместимость со старыми хешами (если нужно)
            if (storedHash.equals("hashed_" + password)) {
                logger.warning("Using legacy hash for user: " + username);
                return user;
            }

        } catch (Exception e) {
            logger.severe("❌ Database error during authentication: " + e.getMessage());
            e.printStackTrace();
        }

        logger.info("=== AUTHENTICATION FAILED ===");
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        logger.warning("⛔ Unauthorized access: " + message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"Math Functions API\"");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        logger.severe("❌ Sending error: " + status + " - " + message);
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}