package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.dto.FunctionAccessDto;
import ru.ssau.tk.pmi.dto.DtoMapper;
import ru.ssau.tk.pmi.repository.manual.FunctionAccessDao;
import ru.ssau.tk.pmi.repository.manual.JdbcFunctionAccessDao;
import ru.ssau.tk.pmi.repository.manual.UserDao;
import ru.ssau.tk.pmi.repository.manual.JdbcUserDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class FunctionAccessServlet extends BaseServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        super.service(request, response);
    }
    private static final Logger logger = Logger.getLogger(FunctionAccessServlet.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== FUNCTION ACCESS API GET REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);
        logger.info("Query Params: " + request.getQueryString());

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            UserDao userDao = new JdbcUserDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/access - получение списка доступов по functionId
                String functionId = request.getParameter("functionId");

                if (functionId == null || functionId.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "functionId parameter required"));
                    logger.warning("VALIDATION: Missing functionId parameter");
                    return;
                }

                Long funcId = Long.parseLong(functionId);
                logger.info("API: GET /api/access - Get access records for function: " + funcId);

                // Получаем все записи доступа из БД
                List<Map<String, Object>> accessDataList = accessDao.getAllAccess();
                List<Map<String, Object>> responseList = new ArrayList<>();

                for (Map<String, Object> accessData : accessDataList) {
                    Long accessFunctionId = (Long) accessData.get("function_id");
                    if (accessFunctionId.equals(funcId)) {
                        Long userId = (Long) accessData.get("user_id");

                        // Получаем username пользователя
                        String username = "user_" + userId; // Заглушка
                        Map<String, Object> userData = userDao.getUserById(userId);
                        if (userData != null) {
                            username = (String) userData.get("username");
                        }

                        Map<String, Object> accessMap = new HashMap<>();
                        accessMap.put("accessId", accessData.get("access_id"));
                        accessMap.put("userId", userId);
                        accessMap.put("username", username);
                        accessMap.put("accessType", accessData.get("access_type"));
                        accessMap.put("grantedAt", new java.util.Date().toString()); // Явно указываем java.util.Date
                        responseList.add(accessMap);
                    }
                }

                mapper.writeValue(response.getWriter(), responseList);
                logger.info("SUCCESS: Returned " + responseList.size() + " access records for function " + funcId);

            } else {
                // GET /api/access/{id} - доступ по ID
                Long accessId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/access/" + accessId + " - Get access by ID");

                Map<String, Object> accessData = accessDao.getAccessById(accessId);
                FunctionAccessDto access = DtoMapper.mapToAccessDto(accessData);

                if (access != null) {
                    mapper.writeValue(response.getWriter(), access);
                    logger.info("SUCCESS: Returned access record: " + accessId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access record not found"));
                    logger.warning("NOT FOUND: Access record ID " + accessId + " not found");
                }
            }
        } catch (NumberFormatException e) {
            logger.severe("Invalid ID format in FunctionAccessServlet GET: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid ID format"));
        } catch (Exception e) {
            logger.severe("ERROR in GET: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Internal server error"));
        }

        logger.info("=== FUNCTION ACCESS API GET COMPLETED ===\n");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== FUNCTION ACCESS API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo == null || pathInfo.equals("/")) {
                // POST /api/access - предоставление доступа
                logger.info("API: POST /api/access - Grant access");

                Long functionId = getLongFromObject(body.get("functionId"));
                Long userId = getLongFromObject(body.get("userId"));
                String accessType = (String) body.get("accessType");

                // Валидация
                if (functionId == null || userId == null || accessType == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of(
                            "error", "functionId, userId and accessType are required"
                    ));
                    logger.warning("VALIDATION: Missing required parameters");
                    return;
                }

                if (!Arrays.asList("READ", "WRITE").contains(accessType.toUpperCase())) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of(
                            "error", "accessType must be READ or WRITE"
                    ));
                    logger.warning("VALIDATION: Invalid access type: " + accessType);
                    return;
                }

                // 🔥 ИСПРАВЛЕНИЕ: Правильная проверка существующей записи в БД
                List<Map<String, Object>> existingAccess = accessDao.getAccessByFunctionAndUser(functionId, userId);
                if (existingAccess != null && !existingAccess.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access already granted"));
                    logger.warning("CONFLICT: Access already exists for user " + userId + " to function " + functionId);
                    return;
                }

                // Создание новой записи доступа в БД
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
                    logger.severe("ERROR: Failed to insert access record into database");
                }

            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
                logger.warning("NOT FOUND: Invalid endpoint " + pathInfo);
            }
        } catch (Exception e) {
            logger.severe("ERROR in POST: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request data"));
        }

        logger.info("=== FUNCTION ACCESS API POST COMPLETED ===\n");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== FUNCTION ACCESS API PUT REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);
            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo != null && !pathInfo.equals("/")) {
                // PUT /api/access/{id} - обновление доступа
                Long accessId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: PUT /api/access/" + accessId + " - Update access");

                Map<String, Object> existingAccessData = accessDao.getAccessById(accessId);
                if (existingAccessData == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access record not found"));
                    logger.warning("NOT FOUND: Access record ID " + accessId + " not found");
                    return;
                }

                String accessType = (String) body.get("accessType");

                if (accessType == null || (!accessType.equals("READ") && !accessType.equals("WRITE"))) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Valid accessType (READ/WRITE) required"));
                    logger.warning("VALIDATION: Invalid access type: " + accessType);
                    return;
                }

                // Обновление записи в БД
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
                logger.warning("VALIDATION: Missing access ID in PUT request");
            }
        } catch (NumberFormatException e) {
            logger.severe("Invalid ID format in FunctionAccessServlet PUT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid access ID format"));
        } catch (Exception e) {
            logger.severe("ERROR in PUT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request data"));
        }

        logger.info("=== FUNCTION ACCESS API PUT COMPLETED ===\n");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        logger.info("=== FUNCTION ACCESS API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            FunctionAccessDao accessDao = new JdbcFunctionAccessDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                // DELETE /api/access - отзыв доступа по functionId и userId
                String functionId = request.getParameter("functionId");
                String userId = request.getParameter("userId");

                logger.info("API: DELETE /api/access - Revoke access by function and user");

                if (functionId == null || userId == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of(
                            "error", "functionId and userId parameters are required"
                    ));
                    logger.warning("VALIDATION: Missing functionId or userId parameters");
                    return;
                }

                Long funcId = Long.parseLong(functionId);
                Long userID = Long.parseLong(userId);

                // 🔥 ИСПРАВЛЕНИЕ: Правильный поиск и удаление записи доступа из БД
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
                    logger.warning("NOT FOUND: No access record found for user " + userID + " and function " + funcId);
                }

            } else {
                // DELETE /api/access/{id} - удаление доступа по ID
                Long accessId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: DELETE /api/access/" + accessId + " - Delete access by ID");

                Map<String, Object> existingAccess = accessDao.getAccessById(accessId);
                if (existingAccess != null) {
                    accessDao.deleteAccess(accessId);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    logger.info("SUCCESS: Deleted access record: " + accessId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access record not found"));
                    logger.warning("NOT FOUND: Access record ID " + accessId + " not found for deletion");
                }
            }
        } catch (NumberFormatException e) {
            logger.severe("Invalid number format in FunctionAccessServlet DELETE: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid number format"));
        } catch (Exception e) {
            logger.severe("ERROR in DELETE: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Internal server error"));
        }

        logger.info("=== FUNCTION ACCESS API DELETE COMPLETED ===\n");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Поддержка CORS для предварительных запросов
        response.setStatus(HttpServletResponse.SC_OK);
    }
}