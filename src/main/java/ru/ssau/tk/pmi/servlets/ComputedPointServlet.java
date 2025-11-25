package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.dto.ComputedPointDto;
import ru.ssau.tk.pmi.dto.DtoMapper;
import ru.ssau.tk.pmi.repository.manual.ComputedPointDao;
import ru.ssau.tk.pmi.repository.manual.JdbcComputedPointDao;
import ru.ssau.tk.pmi.repository.manual.FunctionDao;
import ru.ssau.tk.pmi.repository.manual.JdbcFunctionDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class ComputedPointServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(ComputedPointServlet.class.getName());
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

        logger.info("=== COMPUTED POINTS API GET REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);
        logger.info("Query Params: " + request.getQueryString());

        Map<String, Object> currentUser = getAuthenticatedUser(request);

        try (Connection connection = getConnection()) {
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                logger.info("API: GET /api/points - Search points with filters");

                String functionIdParam = request.getParameter("functionId");
                String xFrom = request.getParameter("xFrom");
                String xTo = request.getParameter("xTo");

                List<Map<String, Object>> pointsData;

                if (functionIdParam != null && !functionIdParam.isEmpty()) {
                    Long funcId = Long.parseLong(functionIdParam);

                    // Проверка доступа к функции
                    Map<String, Object> functionData = functionDao.getFunctionById(funcId);
                    if (functionData != null) {
                        Long ownerId = (Long) functionData.get("owner_id");
                        Boolean isPublic = (Boolean) functionData.get("is_public");

                        if (!isPublic && !hasAccess(request, ownerId)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                            return;
                        }
                    }

                    pointsData = pointDao.getComputedPointsByFunctionId(funcId);
                    logger.info("Filtered by functionId: " + funcId);
                } else {
                    // Получаем все точки, но фильтруем по доступным функциям
                    pointsData = pointDao.getAllComputedPoints();
                    List<Map<String, Object>> accessiblePoints = new ArrayList<>();

                    for (Map<String, Object> point : pointsData) {
                        Long currentFuncId = (Long) point.get("function_id");
                        Map<String, Object> functionData = functionDao.getFunctionById(currentFuncId);
                        if (functionData != null) {
                            Long ownerId = (Long) functionData.get("owner_id");
                            Boolean isPublic = (Boolean) functionData.get("is_public");

                            if (isPublic || hasAccess(request, ownerId)) {
                                accessiblePoints.add(point);
                            }
                        }
                    }
                    pointsData = accessiblePoints;
                }

                // Фильтрация по диапазону X
                List<Map<String, Object>> filteredPoints = new ArrayList<>();
                for (Map<String, Object> pointData : pointsData) {
                    double xValue = (Double) pointData.get("x_value");

                    boolean include = true;
                    if (xFrom != null && !xFrom.isEmpty()) {
                        double xFromVal = Double.parseDouble(xFrom);
                        if (xValue < xFromVal) include = false;
                    }
                    if (xTo != null && !xTo.isEmpty()) {
                        double xToVal = Double.parseDouble(xTo);
                        if (xValue > xToVal) include = false;
                    }

                    if (include) {
                        filteredPoints.add(pointData);
                    }
                }

                mapper.writeValue(response.getWriter(), filteredPoints);
                logger.info("SUCCESS: Returned " + filteredPoints.size() + " points after filtering");

            } else if (pathInfo.equals("/search")) {
                logger.info("API: GET /api/points/search - Search points by coordinates");

                String x = request.getParameter("x");
                String y = request.getParameter("y");
                String functionIdParam = request.getParameter("functionId");

                List<Map<String, Object>> allPoints = pointDao.getAllComputedPoints();
                List<Map<String, Object>> searchResults = new ArrayList<>();

                for (Map<String, Object> point : allPoints) {
                    // Проверка доступа к точке
                    Long currentFuncId = (Long) point.get("function_id");
                    Map<String, Object> functionData = functionDao.getFunctionById(currentFuncId);
                    if (functionData != null) {
                        Long ownerId = (Long) functionData.get("owner_id");
                        Boolean isPublic = (Boolean) functionData.get("is_public");

                        if (!isPublic && !hasAccess(request, ownerId)) {
                            continue; // Пропускаем точки без доступа
                        }
                    }

                    boolean match = true;

                    if (x != null && !x.isEmpty()) {
                        double xVal = Double.parseDouble(x);
                        double pointX = (Double) point.get("x_value");
                        if (Math.abs(pointX - xVal) > 0.001) match = false;
                    }

                    if (y != null && !y.isEmpty()) {
                        double yVal = Double.parseDouble(y);
                        double pointY = (Double) point.get("y_value");
                        if (Math.abs(pointY - yVal) > 0.001) match = false;
                    }

                    if (functionIdParam != null && !functionIdParam.isEmpty()) {
                        Long searchFuncId = Long.parseLong(functionIdParam);
                        Long pointFunctionId = (Long) point.get("function_id");
                        if (!pointFunctionId.equals(searchFuncId)) match = false;
                    }

                    if (match) {
                        searchResults.add(point);
                    }
                }

                mapper.writeValue(response.getWriter(), searchResults);
                logger.info("SUCCESS: Found " + searchResults.size() + " points by search");

            } else {
                Long pointId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/points/" + pointId + " - Get point by ID");

                Map<String, Object> pointData = pointDao.getComputedPointById(pointId);
                if (pointData != null) {
                    // Проверка доступа к точке через функцию
                    Long currentFuncId = (Long) pointData.get("function_id");
                    Map<String, Object> functionData = functionDao.getFunctionById(currentFuncId);
                    if (functionData != null) {
                        Long ownerId = (Long) functionData.get("owner_id");
                        Boolean isPublic = (Boolean) functionData.get("is_public");

                        if (!isPublic && !hasAccess(request, ownerId)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                            return;
                        }
                    }

                    ComputedPointDto point = DtoMapper.mapToComputedPointDto(pointData);
                    mapper.writeValue(response.getWriter(), point);
                    logger.info("SUCCESS: Returned point: " + pointId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Point not found"));
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

        logger.info("=== COMPUTED POINTS API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            logger.info("API: POST /api/points - Create computed point");

            Long functionId = getLongFromObject(body.get("functionId"));
            Double xValue = getDoubleFromObject(body.get("xValue"));
            Double yValue = getDoubleFromObject(body.get("yValue"));

            if (functionId == null || xValue == null || yValue == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "functionId, xValue and yValue required"));
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

            Long pointId = pointDao.insertComputedPoint(functionId, xValue, yValue);

            if (pointId != null) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                mapper.writeValue(response.getWriter(), Map.of(
                        "message", "Point created successfully",
                        "pointId", pointId
                ));
                logger.info("SUCCESS: Created point ID " + pointId + " for function " + functionId);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                mapper.writeValue(response.getWriter(), Map.of("error", "Failed to create point"));
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

        logger.info("=== COMPUTED POINTS API PUT REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo != null && !pathInfo.equals("/")) {
                Long pointId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: PUT /api/points/" + pointId + " - Update point");

                Map<String, Object> existingPoint = pointDao.getComputedPointById(pointId);
                if (existingPoint == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Point not found"));
                    return;
                }

                // Проверка доступа через функцию
                Long currentFuncId = (Long) existingPoint.get("function_id");
                Map<String, Object> functionData = functionDao.getFunctionById(currentFuncId);
                if (functionData != null) {
                    Long ownerId = (Long) functionData.get("owner_id");
                    if (!hasAccess(request, ownerId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                        return;
                    }
                }

                Double yValue = getDoubleFromObject(body.get("yValue"));
                if (yValue == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "yValue is required"));
                    return;
                }

                Double xValue = (Double) existingPoint.get("x_value");
                pointDao.updateComputedPoint(pointId, xValue, yValue);

                mapper.writeValue(response.getWriter(), Map.of("message", "Point updated successfully"));
                logger.info("SUCCESS: Updated point ID " + pointId);

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

        logger.info("=== COMPUTED POINTS API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            if (pathInfo != null && !pathInfo.equals("/")) {
                Long pointId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: DELETE /api/points/" + pointId + " - Delete point");

                Map<String, Object> existingPoint = pointDao.getComputedPointById(pointId);
                if (existingPoint != null) {
                    // Проверка доступа через функцию
                    Long currentFuncId = (Long) existingPoint.get("function_id");
                    Map<String, Object> functionData = functionDao.getFunctionById(currentFuncId);
                    if (functionData != null) {
                        Long ownerId = (Long) functionData.get("owner_id");
                        if (!hasAccess(request, ownerId)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                            return;
                        }
                    }

                    pointDao.deleteComputedPoint(pointId);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    logger.info("SUCCESS: Deleted point: " + pointId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Point not found"));
                }
            }
        } catch (Exception e) {
            logger.severe("ERROR in DELETE: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }
    }
}