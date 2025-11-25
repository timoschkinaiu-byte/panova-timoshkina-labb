package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.dto.UserDto;
import ru.ssau.tk.pmi.dto.DtoMapper;
import ru.ssau.tk.pmi.repository.manual.UserDao;
import ru.ssau.tk.pmi.repository.manual.JdbcUserDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class UserServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(UserServlet.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

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

                List<Map<String, Object>> allUsersData = new ArrayList<>();
                for (long i = 1; i <= 10; i++) {
                    Map<String, Object> userData = userDao.getUserById(i);
                    if (userData != null) {
                        allUsersData.add(userData);
                    }
                }

                List<UserDto> userList = new ArrayList<>();
                for (Map<String, Object> userData : allUsersData) {
                    UserDto user = DtoMapper.mapToUserDto(userData);
                    if (user != null) {
                        boolean include = true;

                        if (search != null && !search.isEmpty()) {
                            if (!user.getUsername().toLowerCase().contains(search.toLowerCase())) {
                                include = false;
                            }
                        }

                        if (role != null && !role.isEmpty()) {
                            if (!user.getRole().equals(role)) {
                                include = false;
                            }
                        }

                        if (include) {
                            userList.add(user);
                        }
                    }
                }

                mapper.writeValue(response.getWriter(), userList);
                logger.info("SUCCESS: Returned " + userList.size() + " users");

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
                userResponse.put("email", "user@example.com");
                userResponse.put("role", currentUser.get("role"));
                userResponse.put("createdAt", new java.util.Date().toString());

                mapper.writeValue(response.getWriter(), userResponse);
                logger.info("SUCCESS: Returned current user info");

            } else {
                Long userId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/users/" + userId + " - User by ID");

                if (!hasAccess(request, userId)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                    return;
                }

                Map<String, Object> userData = userDao.getUserById(userId);
                if (userData != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("userId", userData.get("user_id"));
                    responseData.put("username", userData.get("username"));
                    responseData.put("email", "user@example.com");
                    responseData.put("role", userData.get("role"));
                    responseData.put("createdAt", new java.util.Date().toString());

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

                Map<String, Object> existingUser = userDao.getUserByUsername(username);
                if (existingUser != null) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User already exists"));
                    return;
                }

                String passwordHash = "hashed_" + password;
                String userRole = "USER";
                userDao.insertUser(username, passwordHash, userRole);

                Map<String, Object> newUserData = userDao.getUserByUsername(username);

                response.setStatus(HttpServletResponse.SC_CREATED);
                mapper.writeValue(response.getWriter(), Map.of(
                        "message", "User created successfully",
                        "userId", newUserData.get("user_id"),
                        "role", userRole
                ));
                logger.info("SUCCESS: Created user '" + username + "' with ID " + newUserData.get("user_id"));

            } else if (pathInfo != null && pathInfo.equals("/login")) {
                logger.info("API: POST /api/users/login - User login");

                String username = (String) body.get("username");
                String password = (String) body.get("password");

                Map<String, Object> userData = userDao.getUserByUsername(username);
                if (userData != null) {
                    String storedHash = (String) userData.get("password_hash");
                    if (storedHash.equals("hashed_" + password)) {
                        Map<String, Object> responseData = new HashMap<>();
                        responseData.put("message", "Login successful");
                        responseData.put("user", Map.of(
                                "userId", userData.get("user_id"),
                                "username", userData.get("username"),
                                "role", userData.get("role")
                        ));
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

                if (!hasAccess(request, userId)) {
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

                userDao.updateUser(userId, newUsername, passwordHash, role);

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

                String username = (String) existingUserData.get("username");
                String passwordHash = (String) existingUserData.get("password_hash");
                userDao.updateUser(userId, username, passwordHash, role);

                mapper.writeValue(response.getWriter(), Map.of("message", "Role updated successfully"));
                logger.info("SUCCESS: Changed role for user " + userId + " to " + role);

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

                if (!hasAccess(request, userId)) {
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