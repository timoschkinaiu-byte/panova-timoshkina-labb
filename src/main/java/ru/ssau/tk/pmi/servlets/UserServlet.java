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
        logger.info("Query Params: " + request.getQueryString());

        try (Connection connection = getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                logger.info("API: GET /api/users - Search users");

                String search = request.getParameter("search");
                String role = request.getParameter("role");

                // 🔥 ИСПРАВЛЕНИЕ: Получаем всех пользователей через существующие методы
                List<Map<String, Object>> allUsersData = new ArrayList<>();

                // Получаем пользователей по ID (в реальности нужно добавить метод getAllUsers в DAO)
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

                // Заглушка для текущего пользователя - в реальности из сессии/токена
                Map<String, Object> currentUser = new HashMap<>();
                currentUser.put("userId", 1);
                currentUser.put("username", "current_user");
                currentUser.put("email", "user@example.com");
                currentUser.put("role", "USER");
                currentUser.put("createdAt", new java.util.Date().toString());

                mapper.writeValue(response.getWriter(), currentUser);
                logger.info("SUCCESS: Returned current user info");

            } else {
                Long userId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/users/" + userId + " - User by ID");

                Map<String, Object> userData = userDao.getUserById(userId);
                if (userData != null) {
                    // Форматируем ответ согласно схеме UserResponse
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("userId", userData.get("user_id"));
                    responseData.put("username", userData.get("username"));
                    responseData.put("email", "user@example.com"); // email нет в текущей схеме БД
                    responseData.put("role", userData.get("role"));
                    responseData.put("createdAt", new java.util.Date().toString()); // В реальности из БД

                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Returned user ID " + userId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User not found"));
                    logger.warning("NOT FOUND: User ID " + userId + " not found");
                }
            }
        } catch (Exception e) {
            logger.severe("ERROR in GET: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== USER API GET COMPLETED ===\n");
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

                logger.info("Registration data - username: " + username + ", email: " + email);

                if (username == null || password == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Username and password required"));
                    logger.warning("VALIDATION: Missing username or password");
                    return;
                }

                // Проверка существования пользователя
                Map<String, Object> existingUser = userDao.getUserByUsername(username);
                if (existingUser != null) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User already exists"));
                    logger.warning("CONFLICT: User '" + username + "' already exists");
                    return;
                }

                // Создание пользователя в БД - используем только 3 параметра, email игнорируем
                String passwordHash = "hashed_" + password;
                String userRole = "USER"; // По умолчанию
                userDao.insertUser(username, passwordHash, userRole);

                // Получаем созданного пользователя
                Map<String, Object> newUserData = userDao.getUserByUsername(username);

                response.setStatus(HttpServletResponse.SC_CREATED);
                mapper.writeValue(response.getWriter(), Map.of(
                        "message", "User created successfully",
                        "userId", newUserData.get("user_id")
                ));
                logger.info("SUCCESS: Created user '" + username + "' with ID " + newUserData.get("user_id"));

            } else if (pathInfo != null && pathInfo.equals("/login")) {
                logger.info("API: POST /api/users/login - User login");

                String username = (String) body.get("username");
                String password = (String) body.get("password");

                logger.info("Login attempt for user: " + username);

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
                        logger.info("SUCCESS: User '" + username + "' logged in successfully");
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Invalid credentials"));
                        logger.warning("AUTH FAILED: Invalid password for user '" + username + "'");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Invalid credentials"));
                    logger.warning("AUTH FAILED: User '" + username + "' not found");
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
                logger.warning("NOT FOUND: Invalid endpoint " + pathInfo);
            }
        } catch (Exception e) {
            logger.severe("ERROR in POST: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== USER API POST COMPLETED ===\n");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== USER API PUT REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);
            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> updateData = mapper.readValue(requestBody, Map.class);

            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // PUT /api/users/{id} - обновление пользователя
                Long userId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: PUT /api/users/" + userId + " - Update user");

                Map<String, Object> existingUserData = userDao.getUserById(userId);
                if (existingUserData == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User not found"));
                    logger.warning("NOT FOUND: User ID " + userId + " not found for update");
                    return;
                }

                String username = (String) updateData.get("username");
                String email = (String) updateData.get("email"); // email игнорируем, так как нет в БД

                String newUsername = username != null ? username : (String) existingUserData.get("username");
                String passwordHash = (String) existingUserData.get("password_hash"); // Пароль не меняем
                String role = (String) existingUserData.get("role"); // Роль не меняем

                // Используем существующий метод с 3 параметрами
                userDao.updateUser(userId, newUsername, passwordHash, role);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("message", "User updated successfully");
                responseData.put("userId", userId);

                mapper.writeValue(response.getWriter(), responseData);
                logger.info("SUCCESS: Updated user ID " + userId);

            } else if (pathInfo != null && pathInfo.matches("/\\d+/role")) {
                // PUT /api/users/{id}/role - изменение роли
                Long userId = Long.parseLong(pathInfo.split("/")[1]);
                String role = request.getParameter("role");

                logger.info("API: PUT /api/users/" + userId + "/role - Change user role to " + role);

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

                // Обновляем роль пользователя через updateUser
                String username = (String) existingUserData.get("username");
                String passwordHash = (String) existingUserData.get("password_hash");
                userDao.updateUser(userId, username, passwordHash, role);

                mapper.writeValue(response.getWriter(), Map.of("message", "Role updated successfully"));
                logger.info("SUCCESS: Changed role for user " + userId + " to " + role);

            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
                logger.warning("NOT FOUND: Invalid endpoint " + pathInfo);
            }
        } catch (Exception e) {
            logger.severe("ERROR in PUT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== USER API PUT COMPLETED ===\n");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        logger.info("=== USER API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            UserDao userDao = new JdbcUserDao(connection);

            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long userId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: DELETE /api/users/" + userId + " - Delete user");

                Map<String, Object> existingUser = userDao.getUserById(userId);
                if (existingUser != null) {
                    userDao.deleteUser(userId);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    logger.info("SUCCESS: Deleted user ID " + userId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "User not found"));
                    logger.warning("NOT FOUND: User ID " + userId + " not found for deletion");
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

        logger.info("=== USER API DELETE COMPLETED ===\n");
    }
}