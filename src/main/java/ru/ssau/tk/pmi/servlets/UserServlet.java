package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.dto.UserDto;
import ru.ssau.tk.pmi.dto.DtoMapper;
import ru.ssau.tk.pmi.repository.manual.UserDao;
import ru.ssau.tk.pmi.repository.manual.JdbcUserDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class UserServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(UserServlet.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== USER API GET REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);

        try (Connection connection = getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                logger.info("API: GET /api/users - Search users");

                if (!hasRole(request, "ADMIN")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Admin access required"));
                    return;
                }

                String search = request.getParameter("search");
                String role = request.getParameter("role");
                String enabledParam = request.getParameter("enabled");

                // Вместо getAllUsers() делаем запрос вручную
                List<Map<String, Object>> allUsersData = getAllUsersFromDb(connection);

                List<Map<String, Object>> filteredUsers = new ArrayList<>();
                for (Map<String, Object> userData : allUsersData) {
                    boolean include = true;

                    if (search != null && !search.isEmpty()) {
                        String username = (String) userData.get("username");
                        if (username == null || !username.toLowerCase().contains(search.toLowerCase())) {
                            include = false;
                        }
                    }

                    if (role != null && !role.isEmpty()) {
                        String userRole = (String) userData.get("role");
                        if (userRole == null || !userRole.equals(role)) {
                            include = false;
                        }
                    }

                    if (enabledParam != null && !enabledParam.isEmpty()) {
                        Boolean enabled = (Boolean) userData.get("enabled");
                        boolean enabledBool = Boolean.parseBoolean(enabledParam);
                        if (enabled == null || enabled != enabledBool) {
                            include = false;
                        }
                    }

                    if (include) {
                        Map<String, Object> userResponse = new HashMap<>();
                        userResponse.put("userId", userData.get("user_id"));
                        userResponse.put("username", userData.get("username"));
                        userResponse.put("role", userData.get("role"));
                        userResponse.put("enabled", userData.get("enabled"));

                        Timestamp createdAt = (Timestamp) userData.get("created_at");
                        if (createdAt != null) {
                            userResponse.put("createdAt", createdAt.toLocalDateTime().format(DATE_FORMATTER));
                        } else {
                            userResponse.put("createdAt", LocalDateTime.now().format(DATE_FORMATTER));
                        }

                        filteredUsers.add(userResponse);
                    }
                }

                mapper.writeValue(response.getWriter(), filteredUsers);
                logger.info("SUCCESS: Returned " + filteredUsers.size() + " users");

            } else if (pathInfo.equals("/me")) {
                logger.info("API: GET /api/users/me - Current user");

                if (currentUser == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
                    return;
                }

                Map<String, Object> userResponse = new HashMap<>();
                userResponse.put("userId", currentUser.get("user_id"));
                userResponse.put("username", currentUser.get("username"));
                userResponse.put("role", currentUser.get("role"));
                userResponse.put("enabled", currentUser.get("enabled"));

                Timestamp createdAt = (Timestamp) currentUser.get("created_at");
                if (createdAt != null) {
                    userResponse.put("createdAt", createdAt.toLocalDateTime().format(DATE_FORMATTER));
                } else {
                    userResponse.put("createdAt", LocalDateTime.now().format(DATE_FORMATTER));
                }

                mapper.writeValue(response.getWriter(), userResponse);
                logger.info("SUCCESS: Returned current user info");

            } else {
                Long userId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/users/" + userId + " - User by ID");

                if (!hasAccess(request, userId) && !hasRole(request, "ADMIN")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                    return;
                }

                Map<String, Object> userData = userDao.getUserById(userId);
                if (userData != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("userId", userData.get("user_id"));
                    responseData.put("username", userData.get("username"));
                    responseData.put("role", userData.get("role"));
                    responseData.put("enabled", userData.get("enabled"));

                    Timestamp createdAt = (Timestamp) userData.get("created_at");
                    if (createdAt != null) {
                        responseData.put("createdAt", createdAt.toLocalDateTime().format(DATE_FORMATTER));
                    } else {
                        responseData.put("createdAt", LocalDateTime.now().format(DATE_FORMATTER));
                    }

                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Returned user ID " + userId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User not found"));
                }
            }
        } catch (Exception e) {
            logger.severe("ERROR in GET: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }
    }

    private List<Map<String, Object>> getAllUsersFromDb(Connection connection) {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = "SELECT user_id, username, role, enabled, created_at FROM users ORDER BY user_id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("user_id", rs.getLong("user_id"));
                user.put("username", rs.getString("username"));
                user.put("role", rs.getString("role"));
                user.put("enabled", rs.getBoolean("enabled"));
                user.put("created_at", rs.getTimestamp("created_at"));
                users.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all users", e);
        }
        return users;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== USER API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);
            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo != null && pathInfo.equals("/register")) {
                logger.info("API: POST /api/users/register - User registration");

                String username = (String) body.get("username");
                String password = (String) body.get("password");
                String email = (String) body.get("email");

                if (username == null || password == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Username and password required"));
                    return;
                }

                if (password.length() < 6) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Password must be at least 6 characters"));
                    return;
                }

                Map<String, Object> existingUser = userDao.getUserByUsername(username);
                if (existingUser != null) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User already exists"));
                    return;
                }

                String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
                String userRole = "USER";
                Boolean enabled = true;
                userDao.insertUser(username, passwordHash, userRole, enabled);

                Map<String, Object> newUserData = userDao.getUserByUsername(username);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("message", "User created successfully");
                responseData.put("userId", newUserData.get("user_id"));
                responseData.put("username", newUserData.get("username"));
                responseData.put("role", userRole);
                responseData.put("enabled", enabled);

                response.setStatus(HttpServletResponse.SC_CREATED);
                mapper.writeValue(response.getWriter(), responseData);
                logger.info("SUCCESS: Created user '" + username + "' with ID " + newUserData.get("user_id"));

            } else if (pathInfo != null && pathInfo.equals("/login")) {
                logger.info("API: POST /api/users/login - User login");

                String username = (String) body.get("username");
                String password = (String) body.get("password");

                Map<String, Object> userData = userDao.getUserByUsername(username);
                if (userData != null) {
                    String storedHash = (String) userData.get("password_hash");
                    if (storedHash != null && BCrypt.checkpw(password, storedHash)) {
                        Boolean enabled = (Boolean) userData.get("enabled");
                        if (enabled == null || !enabled) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            mapper.writeValue(response.getWriter(), Map.of("error", "User account is disabled"));
                            logger.warning("Disabled user attempted login: " + username);
                            return;
                        }

                        Map<String, Object> userResponse = new HashMap<>();
                        userResponse.put("userId", userData.get("user_id"));
                        userResponse.put("username", userData.get("username"));
                        userResponse.put("role", userData.get("role"));
                        userResponse.put("enabled", enabled);

                        Timestamp createdAt = (Timestamp) userData.get("created_at");
                        if (createdAt != null) {
                            userResponse.put("createdAt", createdAt.toLocalDateTime().format(DATE_FORMATTER));
                        }

                        Map<String, Object> responseData = new HashMap<>();
                        responseData.put("message", "Login successful");
                        responseData.put("user", userResponse);

                        mapper.writeValue(response.getWriter(), responseData);
                        logger.info("SUCCESS: User '" + username + "' logged in");
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Invalid credentials"));
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Invalid credentials"));
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
            }
        } catch (Exception e) {
            logger.severe("ERROR in POST: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== USER API PUT REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);
            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> updateData = mapper.readValue(requestBody, Map.class);

            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long userId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: PUT /api/users/" + userId + " - Update user");

                if (!hasAccess(request, userId) && !hasRole(request, "ADMIN")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                    return;
                }

                Map<String, Object> existingUserData = userDao.getUserById(userId);
                if (existingUserData == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User not found"));
                    return;
                }

                String username = (String) updateData.get("username");
                String newUsername = username != null ? username : (String) existingUserData.get("username");
                String passwordHash = (String) existingUserData.get("password_hash");
                String role = (String) existingUserData.get("role");
                Boolean enabled = (Boolean) existingUserData.get("enabled");

                if (username != null && !username.equals(existingUserData.get("username"))) {
                    Map<String, Object> userWithSameUsername = userDao.getUserByUsername(username);
                    if (userWithSameUsername != null) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Username already taken"));
                        return;
                    }
                }

                userDao.updateUser(userId, newUsername, passwordHash, role, enabled);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("message", "User updated successfully");
                responseData.put("userId", userId);

                mapper.writeValue(response.getWriter(), responseData);
                logger.info("SUCCESS: Updated user ID " + userId);

            } else if (pathInfo != null && pathInfo.matches("/\\d+/role")) {
                Long userId = Long.parseLong(pathInfo.split("/")[1]);
                String role = request.getParameter("role");

                logger.info("API: PUT /api/users/" + userId + "/role - Change user role to " + role);

                if (!hasRole(request, "ADMIN")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Admin access required"));
                    return;
                }

                Map<String, Object> existingUserData = userDao.getUserById(userId);
                if (existingUserData == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User not found"));
                    return;
                }

                if (role == null || (!role.equals("USER") && !role.equals("ADMIN"))) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Valid role (USER/ADMIN) required"));
                    return;
                }

                userDao.updateUserRole(userId, role);

                mapper.writeValue(response.getWriter(), Map.of(
                        "message", "Role updated successfully",
                        "userId", userId,
                        "role", role
                ));
                logger.info("SUCCESS: Changed role for user " + userId + " to " + role);

            } else if (pathInfo != null && pathInfo.matches("/\\d+/enabled")) {
                Long userId = Long.parseLong(pathInfo.split("/")[1]);
                Boolean enabled = getBooleanFromObject(updateData.get("enabled"));

                logger.info("API: PUT /api/users/" + userId + "/enabled - Set enabled to " + enabled);

                if (!hasRole(request, "ADMIN")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Admin access required"));
                    return;
                }

                if (enabled == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "enabled parameter required"));
                    return;
                }

                userDao.updateEnabledStatus(userId, enabled);

                mapper.writeValue(response.getWriter(), Map.of(
                        "message", "User enabled status updated",
                        "userId", userId,
                        "enabled", enabled
                ));
                logger.info("SUCCESS: Updated enabled status for user " + userId + " to " + enabled);

            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
            }
        } catch (Exception e) {
            logger.severe("ERROR in PUT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        logger.info("=== USER API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);

            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long userId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: DELETE /api/users/" + userId + " - Delete user");

                if (!hasAccess(request, userId) && !hasRole(request, "ADMIN")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                    return;
                }

                Map<String, Object> existingUser = userDao.getUserById(userId);
                if (existingUser != null) {
                    userDao.deleteUser(userId);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    logger.info("SUCCESS: Deleted user ID " + userId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User not found"));
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
            }
        } catch (Exception e) {
            logger.severe("ERROR in DELETE: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }
    }
}