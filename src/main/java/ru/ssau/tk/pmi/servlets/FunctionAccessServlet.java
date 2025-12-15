package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.repository.manual.FunctionAccessDao;
import ru.ssau.tk.pmi.repository.manual.JdbcFunctionAccessDao;
import ru.ssau.tk.pmi.repository.manual.UserDao;
import ru.ssau.tk.pmi.repository.manual.JdbcUserDao;
import ru.ssau.tk.pmi.repository.manual.FunctionDao;
import ru.ssau.tk.pmi.repository.manual.JdbcFunctionDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class FunctionAccessServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(FunctionAccessServlet.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        logger.info("=== FUNCTION ACCESS API GET REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Query Params: " + request.getQueryString());

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        String functionIdParam = request.getParameter("functionId");
        if (functionIdParam == null || functionIdParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "functionId parameter required"));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            UserDao userDao = new JdbcUserDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            Long funcId = Long.parseLong(functionIdParam);
            logger.info("API: GET /api/access - Get access records for function: " + funcId);

            // Проверка доступа к функции
            Map<String, Object> functionData = functionDao.getFunctionById(funcId);
            if (functionData == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                return;
            }

            Long ownerId = (Long) functionData.get("owner_id");
            if (!hasAccess(request, ownerId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                return;
            }

            List<Map<String, Object>> accessDataList = accessDao.getAllAccess();
            List<Map<String, Object>> responseList = new ArrayList<>();

            for (Map<String, Object> accessData : accessDataList) {
                Long accessFunctionId = (Long) accessData.get("function_id");
                if (accessFunctionId.equals(funcId)) {
                    Long userId = (Long) accessData.get("user_id");

                    String username = "user_" + userId;
                    Map<String, Object> userData = userDao.getUserById(userId);
                    if (userData != null) {
                        username = (String) userData.get("username");
                    }

                    Map<String, Object> accessMap = new HashMap<>();
                    accessMap.put("accessId", accessData.get("access_id"));
                    accessMap.put("userId", userId);
                    accessMap.put("username", username);
                    accessMap.put("accessType", accessData.get("access_type"));
                    accessMap.put("grantedAt", LocalDateTime.now().format(DATE_FORMATTER));
                    responseList.add(accessMap);
                }
            }

            mapper.writeValue(response.getWriter(), responseList);
            logger.info("SUCCESS: Returned " + responseList.size() + " access records for function " + funcId);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid functionId format"));
        } catch (Exception e) {
            logger.severe("ERROR in GET: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Server error"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        logger.info("=== FUNCTION ACCESS API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            UserDao userDao = new JdbcUserDao(connection);

            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            logger.info("API: POST /api/access - Grant access");

            Long functionId = getLongFromObject(body.get("functionId"));
            Long userId = getLongFromObject(body.get("userId"));
            String accessType = (String) body.get("accessType");

            if (functionId == null || userId == null || accessType == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of(
                        "error", "functionId, userId and accessType are required"
                ));
                return;
            }

            if (!Arrays.asList("READ", "WRITE").contains(accessType.toUpperCase())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of(
                        "error", "accessType must be READ or WRITE"
                ));
                return;
            }

            // Проверка доступа к функции
            Map<String, Object> functionData = functionDao.getFunctionById(functionId);
            if (functionData == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                return;
            }

            Long ownerId = (Long) functionData.get("owner_id");
            if (!hasAccess(request, ownerId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                return;
            }

            // Проверка существования пользователя
            Map<String, Object> userData = userDao.getUserById(userId);
            if (userData == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "User not found"));
                return;
            }

            List<Map<String, Object>> existingAccess = accessDao.getAccessByFunctionAndUser(functionId, userId);
            if (existingAccess != null && !existingAccess.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access already granted"));
                return;
            }

            Long accessId = accessDao.insertAccess(functionId, userId, accessType.toUpperCase());

            if (accessId != null) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("accessId", accessId);
                responseData.put("userId", userId);
                responseData.put("username", userData.get("username"));
                responseData.put("accessType", accessType);
                responseData.put("grantedAt", LocalDateTime.now().format(DATE_FORMATTER));

                response.setStatus(HttpServletResponse.SC_CREATED);
                mapper.writeValue(response.getWriter(), responseData);
                logger.info("SUCCESS: Granted " + accessType + " access to user " + userId + " for function " + functionId);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                mapper.writeValue(response.getWriter(), Map.of("error", "Failed to grant access"));
            }

        } catch (Exception e) {
            logger.severe("ERROR in POST: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Server error"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        logger.info("=== FUNCTION ACCESS API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Query Params: " + request.getQueryString());

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        String functionIdParam = request.getParameter("functionId");
        String userIdParam = request.getParameter("userId");

        if (functionIdParam == null || userIdParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of(
                    "error", "functionId and userId parameters are required"
            ));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            Long funcId = Long.parseLong(functionIdParam);
            Long userID = Long.parseLong(userIdParam);

            logger.info("API: DELETE /api/access - Revoke access for user " + userID + " from function " + funcId);

            // Проверка доступа к функции
            Map<String, Object> functionData = functionDao.getFunctionById(funcId);
            if (functionData == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                return;
            }

            Long ownerId = (Long) functionData.get("owner_id");
            if (!hasAccess(request, ownerId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                return;
            }

            List<Map<String, Object>> accessRecords = accessDao.getAccessByFunctionAndUser(funcId, userID);
            if (accessRecords != null && !accessRecords.isEmpty()) {
                for (Map<String, Object> accessRecord : accessRecords) {
                    Long accessId = (Long) accessRecord.get("access_id");
                    accessDao.deleteAccess(accessId);
                }
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                logger.info("SUCCESS: Revoked access for user " + userID + " to function " + funcId);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access record not found"));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid functionId or userId format"));
        } catch (Exception e) {
            logger.severe("ERROR in DELETE: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Server error"));
        }
    }
}