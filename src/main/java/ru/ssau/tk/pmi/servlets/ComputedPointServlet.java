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
                logger.info("API: GET /api/points - Get points with filters");

                Long functionId = getLongFromObject(request.getParameter("functionId"));
                Double xFrom = getDoubleFromObject(request.getParameter("xFrom"));
                Double xTo = getDoubleFromObject(request.getParameter("xTo"));

                if (functionId == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "functionId parameter is required"));
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
                Boolean isPublic = (Boolean) functionData.get("is_public");

                if (!isPublic && !hasAccess(request, ownerId)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                    return;
                }

                List<Map<String, Object>> pointsData = pointDao.getComputedPointsByFunctionId(functionId);
                List<Map<String, Object>> filteredPoints = new ArrayList<>();

                // Фильтрация по диапазону X
                for (Map<String, Object> pointData : pointsData) {
                    double xValue = (Double) pointData.get("x_value");

                    boolean include = true;
                    if (xFrom != null && xValue < xFrom) include = false;
                    if (xTo != null && xValue > xTo) include = false;

                    if (include) {
                        Map<String, Object> pointMap = new HashMap<>();
                        pointMap.put("pointId", pointData.get("point_id"));
                        pointMap.put("functionId", pointData.get("function_id"));
                        pointMap.put("xValue", pointData.get("x_value"));
                        pointMap.put("yValue", pointData.get("y_value"));
                        filteredPoints.add(pointMap);
                    }
                }

                mapper.writeValue(response.getWriter(), filteredPoints);
                logger.info("SUCCESS: Returned " + filteredPoints.size() + " points for function " + functionId);

            } else if (pathInfo.equals("/search")) {
                logger.info("API: GET /api/points/search - Search points");

                Double x = getDoubleFromObject(request.getParameter("x"));
                Double y = getDoubleFromObject(request.getParameter("y"));
                Long functionId = getLongFromObject(request.getParameter("functionId"));

                List<Map<String, Object>> allPoints = pointDao.getAllComputedPoints();
                List<Map<String, Object>> searchResults = new ArrayList<>();

                for (Map<String, Object> point : allPoints) {
                    Long currentFuncId = (Long) point.get("function_id");

                    // Проверка доступа
                    Map<String, Object> functionData = functionDao.getFunctionById(currentFuncId);
                    if (functionData != null) {
                        Long ownerId = (Long) functionData.get("owner_id");
                        Boolean isPublic = (Boolean) functionData.get("is_public");

                        if (!isPublic && !hasAccess(request, ownerId)) {
                            continue;
                        }
                    }

                    boolean match = true;

                    if (functionId != null && !functionId.equals(currentFuncId)) {
                        match = false;
                    }

                    if (x != null) {
                        double pointX = (Double) point.get("x_value");
                        if (Math.abs(pointX - x) > 0.0001) match = false;
                    }

                    if (y != null) {
                        double pointY = (Double) point.get("y_value");
                        if (Math.abs(pointY - y) > 0.0001) match = false;
                    }

                    if (match) {
                        Map<String, Object> pointMap = new HashMap<>();
                        pointMap.put("pointId", point.get("point_id"));
                        pointMap.put("functionId", point.get("function_id"));
                        pointMap.put("xValue", point.get("x_value"));
                        pointMap.put("yValue", point.get("y_value"));
                        searchResults.add(pointMap);
                    }
                }

                mapper.writeValue(response.getWriter(), searchResults);
                logger.info("SUCCESS: Found " + searchResults.size() + " points");

            } else {
                Long pointId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/points/" + pointId + " - Get point by ID");

                Map<String, Object> pointData = pointDao.getComputedPointById(pointId);
                if (pointData != null) {
                    Long functionId = (Long) pointData.get("function_id");
                    Map<String, Object> functionData = functionDao.getFunctionById(functionId);

                    if (functionData != null) {
                        Long ownerId = (Long) functionData.get("owner_id");
                        Boolean isPublic = (Boolean) functionData.get("is_public");

                        if (!isPublic && !hasAccess(request, ownerId)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                            return;
                        }
                    }

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("pointId", pointData.get("point_id"));
                    responseData.put("functionId", pointData.get("function_id"));
                    responseData.put("xValue", pointData.get("x_value"));
                    responseData.put("yValue", pointData.get("y_value"));

                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Returned point: " + pointId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Point not found"));
                }
            }
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
            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            logger.info("API: POST /api/points - Create computed point");

            Long functionId = getLongFromObject(body.get("functionId"));
            Double xValue = getDoubleFromObject(body.get("xValue"));
            Double yValue = getDoubleFromObject(body.get("yValue"));

            if (functionId == null || xValue == null || yValue == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "functionId, xValue and yValue are required"));
                return;
            }

            // Проверка доступа
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

// ========== ДОБАВЛЕННЫЙ КОД ==========
// Проверка на дубликат X
            List<Map<String, Object>> existingPoints = pointDao.getComputedPointsByFunctionId(functionId);
            for (Map<String, Object> existingPoint : existingPoints) {
                double existingX = (Double) existingPoint.get("x_value");
                if (Math.abs(existingX - xValue) < 0.000001) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    mapper.writeValue(response.getWriter(), Map.of(
                            "error", "Point with X=" + xValue + " already exists for this function"
                    ));
                    logger.warning("Duplicate point creation attempt: function=" + functionId + ", x=" + xValue);
                    return;
                }
            }
// =====================================

            Long pointId = pointDao.insertComputedPoint(functionId, xValue, yValue);

            if (pointId != null) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("pointId", pointId);
                responseData.put("functionId", functionId);
                responseData.put("xValue", xValue);
                responseData.put("yValue", yValue);

                response.setStatus(HttpServletResponse.SC_CREATED);
                mapper.writeValue(response.getWriter(), responseData);
                logger.info("SUCCESS: Created point ID " + pointId);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                mapper.writeValue(response.getWriter(), Map.of("error", "Failed to create point"));
            }

        } catch (Exception e) {
            logger.severe("ERROR in POST: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Server error"));
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

                // Проверка доступа
                Long functionId = (Long) existingPoint.get("function_id");
                Map<String, Object> functionData = functionDao.getFunctionById(functionId);
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

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("pointId", pointId);
                responseData.put("functionId", functionId);
                responseData.put("xValue", xValue);
                responseData.put("yValue", yValue);

                mapper.writeValue(response.getWriter(), responseData);
                logger.info("SUCCESS: Updated point ID " + pointId);

            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "Point ID required"));
            }
        } catch (Exception e) {
            logger.severe("ERROR in PUT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Server error"));
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
                    // Проверка доступа
                    Long functionId = (Long) existingPoint.get("function_id");
                    Map<String, Object> functionData = functionDao.getFunctionById(functionId);
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
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "Point ID required"));
            }
        } catch (Exception e) {
            logger.severe("ERROR in DELETE: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Server error"));
        }
    }
}