package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.dto.FunctionAccessDto;
import ru.ssau.tk.pmi.dto.DtoMapper;
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
import java.util.*;
import java.util.logging.Logger;

public class FunctionAccessServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(FunctionAccessServlet.class.getName());
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

        logger.info("=== FUNCTION ACCESS API GET REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            UserDao userDao = new JdbcUserDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                String functionId = request.getParameter("functionId");

                if (functionId == null || functionId.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "functionId parameter required"));
                    return;
                }

                Long funcId = Long.parseLong(functionId);
                logger.info("API: GET /api/access - Get access records for function: " + funcId);

                // Проверка доступа к функции
                Map<String, Object> functionData = functionDao.getFunctionById(funcId);
                if (functionData != null) {
                    Long ownerId = (Long) functionData.get("owner_id");
                    if (!hasAccess(request, ownerId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                        return;
                    }
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
                        accessMap.put("grantedAt", new java.util.Date().toString());
                        responseList.add(accessMap);
                    }
                }

                mapper.writeValue(response.getWriter(), responseList);
                logger.info("SUCCESS: Returned " + responseList.size() + " access records for function " + funcId);

            } else {
                Long accessId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/access/" + accessId + " - Get access by ID");

                Map<String, Object> accessData = accessDao.getAccessById(accessId);
                if (accessData != null) {
                    // Проверка доступа через функцию
                    Long funcId = (Long) accessData.get("function_id");
                    Map<String, Object> functionData = functionDao.getFunctionById(funcId);
                    if (functionData != null) {
                        Long ownerId = (Long) functionData.get("owner_id");
                        if (!hasAccess(request, ownerId)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                            return;
                        }
                    }

                    FunctionAccessDto access = DtoMapper.mapToAccessDto(accessData);
                    mapper.writeValue(response.getWriter(), access);
                    logger.info("SUCCESS: Returned access record: " + accessId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access record not found"));
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

        logger.info("=== FUNCTION ACCESS API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo == null || pathInfo.equals("/")) {
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

                List<Map<String, Object>> existingAccess = accessDao.getAccessByFunctionAndUser(functionId, userId);
                if (existingAccess != null && !existingAccess.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access already granted"));
                    return;
                }

                Long accessId = accessDao.insertAccess(functionId, userId, accessType.toUpperCase());

                if (accessId != null) {
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    mapper.writeValue(response.getWriter(), Map.of(
                            "message", "Access granted successfully",
                            "accessId", accessId,
                            "functionId", functionId,
                            "userId", userId,
                            "accessType", accessType
                    ));
                    logger.info("SUCCESS: Granted " + accessType + " access to user " + userId + " for function " + functionId);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Failed to grant access"));
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

        logger.info("=== FUNCTION ACCESS API PUT REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo != null && !pathInfo.equals("/")) {
                Long accessId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: PUT /api/access/" + accessId + " - Update access");

                Map<String, Object> existingAccessData = accessDao.getAccessById(accessId);
                if (existingAccessData == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access record not found"));
                    return;
                }

                // Проверка доступа через функцию
                Long funcId = (Long) existingAccessData.get("function_id");
                Map<String, Object> functionData = functionDao.getFunctionById(funcId);
                if (functionData != null) {
                    Long ownerId = (Long) functionData.get("owner_id");
                    if (!hasAccess(request, ownerId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                        return;
                    }
                }

                String accessType = (String) body.get("accessType");

                if (accessType == null || (!accessType.equals("READ") && !accessType.equals("WRITE"))) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Valid accessType (READ/WRITE) required"));
                    return;
                }

                accessDao.updateAccess(accessId, accessType);

                mapper.writeValue(response.getWriter(), Map.of(
                        "message", "Access updated successfully",
                        "accessId", accessId,
                        "accessType", accessType
                ));
                logger.info("SUCCESS: Updated access " + accessId + " to type: " + accessType);

            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access ID required"));
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

        logger.info("=== FUNCTION ACCESS API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                String functionId = request.getParameter("functionId");
                String userId = request.getParameter("userId");

                logger.info("API: DELETE /api/access - Revoke access by function and user");

                if (functionId == null || userId == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of(
                            "error", "functionId and userId parameters are required"
                    ));
                    return;
                }

                Long funcId = Long.parseLong(functionId);
                Long userID = Long.parseLong(userId);

                // Проверка доступа к функции
                Map<String, Object> functionData = functionDao.getFunctionById(funcId);
                if (functionData != null) {
                    Long ownerId = (Long) functionData.get("owner_id");
                    if (!hasAccess(request, ownerId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                        return;
                    }
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

            } else {
                Long accessId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: DELETE /api/access/" + accessId + " - Delete access by ID");

                Map<String, Object> existingAccess = accessDao.getAccessById(accessId);
                if (existingAccess != null) {
                    // Проверка доступа через функцию
                    Long funcId = (Long) existingAccess.get("function_id");
                    Map<String, Object> functionData = functionDao.getFunctionById(funcId);
                    if (functionData != null) {
                        Long ownerId = (Long) functionData.get("owner_id");
                        if (!hasAccess(request, ownerId)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                            return;
                        }
                    }

                    accessDao.deleteAccess(accessId);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    logger.info("SUCCESS: Deleted access record: " + accessId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access record not found"));
                }
            }
        } catch (Exception e) {
            logger.severe("ERROR in DELETE: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }
    }
}