package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.dto.FunctionDto;
import ru.ssau.tk.pmi.dto.DtoMapper;
import ru.ssau.tk.pmi.repository.manual.FunctionDao;
import ru.ssau.tk.pmi.repository.manual.JdbcFunctionDao;
import ru.ssau.tk.pmi.repository.manual.ComputedPointDao;
import ru.ssau.tk.pmi.repository.manual.JdbcComputedPointDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class FunctionServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(FunctionServlet.class.getName());
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

        logger.info("=== FUNCTION API GET REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);

        try (Connection connection = getConnection()) {
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                logger.info("API: GET /api/functions - Search functions");

                List<Map<String, Object>> functionsData = functionDao.getAllFunctions();
                List<Map<String, Object>> functionList = new ArrayList<>();

                for (Map<String, Object> functionData : functionsData) {
                    Long ownerId = (Long) functionData.get("owner_id");
                    Boolean isPublic = (Boolean) functionData.get("is_public");

                    if (isPublic || hasAccess(request, ownerId)) {
                        Map<String, Object> functionMap = new HashMap<>();
                        functionMap.put("functionId", functionData.get("function_id"));
                        functionMap.put("functionName", functionData.get("function_name"));
                        functionMap.put("functionType", "TABULATED");
                        functionMap.put("ownerId", ownerId);
                        functionMap.put("isPublic", isPublic);
                        functionMap.put("pointsCount", pointDao.getComputedPointsByFunctionId((Long)functionData.get("function_id")).size());
                        functionMap.put("createdAt", new java.util.Date().toString());
                        functionList.add(functionMap);
                    }
                }

                String search = request.getParameter("search");
                String type = request.getParameter("type");
                String ownerId = request.getParameter("ownerId");
                String isPublic = request.getParameter("isPublic");

                if (search != null && !search.isEmpty()) {
                    functionList.removeIf(func ->
                            !((String)func.get("functionName")).toLowerCase().contains(search.toLowerCase()));
                }

                if (type != null && !type.isEmpty()) {
                    functionList.removeIf(func -> !func.get("functionType").equals(type));
                }

                if (ownerId != null && !ownerId.isEmpty()) {
                    Long ownerIdLong = Long.parseLong(ownerId);
                    functionList.removeIf(func -> !func.get("ownerId").equals(ownerIdLong));
                }

                if (isPublic != null && !isPublic.isEmpty()) {
                    boolean isPublicBool = Boolean.parseBoolean(isPublic);
                    functionList.removeIf(func -> !func.get("isPublic").equals(isPublicBool));
                }

                mapper.writeValue(response.getWriter(), functionList);
                logger.info("SUCCESS: Returned " + functionList.size() + " functions");

            } else if (pathInfo.matches("/\\d+/points")) {
                Long functionId = Long.parseLong(pathInfo.split("/")[1]);
                logger.info("API: GET /api/functions/" + functionId + "/points - Get function points");

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
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                    return;
                }

                List<Map<String, Object>> pointsData = pointDao.getComputedPointsByFunctionId(functionId);
                List<Map<String, Object>> responsePoints = new ArrayList<>();

                for (Map<String, Object> point : pointsData) {
                    Map<String, Object> pointMap = new HashMap<>();
                    pointMap.put("pointId", point.get("point_id"));
                    pointMap.put("functionId", point.get("function_id"));
                    pointMap.put("xValue", point.get("x_value"));
                    pointMap.put("yValue", point.get("y_value"));
                    responsePoints.add(pointMap);
                }

                mapper.writeValue(response.getWriter(), responsePoints);
                logger.info("SUCCESS: Returned " + responsePoints.size() + " points for function " + functionId);

            } else if (pathInfo.matches("/\\d+/graph-data")) {
                Long functionId = Long.parseLong(pathInfo.split("/")[1]);
                logger.info("API: GET /api/functions/" + functionId + "/graph-data - Get graph data");

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
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                    return;
                }

                List<Map<String, Object>> pointsData = pointDao.getComputedPointsByFunctionId(functionId);
                Map<String, Object> graphData = generateGraphData(functionId, functionData, pointsData);
                mapper.writeValue(response.getWriter(), graphData);
                logger.info("SUCCESS: Generated graph data for function " + functionId);

            } else if (pathInfo.matches("/\\d+")) {
                Long functionId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/functions/" + functionId + " - Get function by ID");

                Map<String, Object> functionData = functionDao.getFunctionById(functionId);
                if (functionData != null) {
                    Long ownerId = (Long) functionData.get("owner_id");
                    Boolean isPublic = (Boolean) functionData.get("is_public");

                    if (!isPublic && !hasAccess(request, ownerId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                        return;
                    }

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("functionId", functionData.get("function_id"));
                    responseData.put("functionName", functionData.get("function_name"));
                    responseData.put("functionType", "TABULATED");
                    responseData.put("ownerId", ownerId);
                    responseData.put("isPublic", isPublic);
                    responseData.put("pointsCount", pointDao.getComputedPointsByFunctionId(functionId).size());
                    responseData.put("createdAt", new java.util.Date().toString());

                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Returned function ID " + functionId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
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

        logger.info("=== FUNCTION API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        Long currentUserId = (Long) currentUser.get("user_id");

        try (Connection connection = getConnection()) {
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);

            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo == null || pathInfo.equals("/")) {
                logger.info("API: POST /api/functions - Create function");

                String functionName = (String) body.get("name");
                String functionDefinition = (String) body.get("definition");
                Long ownerId = getLongFromObject(body.get("ownerId"));
                Boolean isPublic = getBooleanFromObject(body.get("isPublic"));

                if (functionName == null || functionDefinition == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Function name and definition required"));
                    return;
                }

                if (ownerId != null && !ownerId.equals(currentUserId) && !hasRole(request, "ADMIN")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Cannot create functions for other users"));
                    return;
                }

                Long finalOwnerId = ownerId != null ? ownerId : currentUserId;
                Long functionId = functionDao.insertFunction(functionName, functionDefinition,
                        finalOwnerId, isPublic != null ? isPublic : false);

                if (functionId != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("functionId", functionId);
                    responseData.put("functionName", functionName);
                    responseData.put("functionType", "TABULATED");
                    responseData.put("ownerId", finalOwnerId);
                    responseData.put("isPublic", isPublic != null ? isPublic : false);
                    responseData.put("pointsCount", 0);
                    responseData.put("createdAt", new java.util.Date().toString());

                    response.setStatus(HttpServletResponse.SC_CREATED);
                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Created function '" + functionName + "' with ID " + functionId);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Failed to create function"));
                }

            } else if (pathInfo.equals("/from-arrays")) {
                logger.info("API: POST /api/functions/from-arrays - Create function from arrays");

                String name = (String) body.get("name");
                List<Double> xValues = (List<Double>) body.get("xValues");
                List<Double> yValues = (List<Double>) body.get("yValues");

                if (name == null || xValues == null || yValues == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Name, xValues and yValues required"));
                    return;
                }

                String definition = "Array function with " + xValues.size() + " points";
                Long functionId = functionDao.insertFunction(name, definition, currentUserId, true);

                if (functionId != null) {
                    for (int i = 0; i < xValues.size(); i++) {
                        pointDao.insertComputedPoint(functionId, xValues.get(i), yValues.get(i));
                    }

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("functionId", functionId);
                    responseData.put("functionName", name);
                    responseData.put("functionType", "TABULATED");
                    responseData.put("ownerId", currentUserId);
                    responseData.put("isPublic", true);
                    responseData.put("pointsCount", xValues.size());
                    responseData.put("createdAt", new java.util.Date().toString());

                    response.setStatus(HttpServletResponse.SC_CREATED);
                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Created array function '" + name + "' with " + xValues.size() + " points");
                }

            } else if (pathInfo.equals("/from-math-function")) {
                logger.info("API: POST /api/functions/from-math-function - Create from math function");

                String name = (String) body.get("name");
                String sourceFunctionName = (String) body.get("sourceFunctionName");
                Double leftX = getDoubleFromObject(body.get("leftX"));
                Double rightX = getDoubleFromObject(body.get("rightX"));
                Integer pointsCount = (Integer) body.get("pointsCount");

                if (name == null || sourceFunctionName == null || leftX == null || rightX == null || pointsCount == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "All parameters required"));
                    return;
                }

                String definition = "Math function: " + sourceFunctionName + " from " + leftX + " to " + rightX;
                Long functionId = functionDao.insertFunction(name, definition, currentUserId, true);

                if (functionId != null) {
                    double step = (rightX - leftX) / (pointsCount - 1);
                    for (int i = 0; i < pointsCount; i++) {
                        double x = leftX + i * step;
                        double y = computeMathFunction(sourceFunctionName, x);
                        pointDao.insertComputedPoint(functionId, x, y);
                    }

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("functionId", functionId);
                    responseData.put("functionName", name);
                    responseData.put("functionType", "TABULATED");
                    responseData.put("ownerId", currentUserId);
                    responseData.put("isPublic", true);
                    responseData.put("pointsCount", pointsCount);
                    responseData.put("createdAt", new java.util.Date().toString());

                    response.setStatus(HttpServletResponse.SC_CREATED);
                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Created math function '" + name + "' with " + pointsCount + " points");
                }

            } else if (pathInfo.equals("/composite")) {
                logger.info("API: POST /api/functions/composite - Create composite function");

                String name = (String) body.get("name");
                String outerFunctionName = (String) body.get("outerFunctionName");
                String innerFunctionName = (String) body.get("innerFunctionName");

                if (name == null || outerFunctionName == null || innerFunctionName == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "All parameters required"));
                    return;
                }

                String definition = "Composite: " + outerFunctionName + "(" + innerFunctionName + "(x))";
                Long functionId = functionDao.insertFunction(name, definition, currentUserId, true);

                if (functionId != null) {
                    for (double x = -10; x <= 10; x += 0.5) {
                        double innerY = computeMathFunction(innerFunctionName, x);
                        double y = computeMathFunction(outerFunctionName, innerY);
                        pointDao.insertComputedPoint(functionId, x, y);
                    }

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("functionId", functionId);
                    responseData.put("functionName", name);
                    responseData.put("functionType", "TABULATED");
                    responseData.put("ownerId", currentUserId);
                    responseData.put("isPublic", true);
                    responseData.put("pointsCount", 41);
                    responseData.put("createdAt", new java.util.Date().toString());

                    response.setStatus(HttpServletResponse.SC_CREATED);
                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Created composite function '" + name + "'");
                }

            } else if (pathInfo.matches("/\\d+/compute")) {
                Long functionId = Long.parseLong(pathInfo.split("/")[1]);
                logger.info("API: POST /api/functions/" + functionId + "/compute - Compute function value");

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
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                    return;
                }

                Double x = getDoubleFromObject(body.get("x"));
                if (x == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "X coordinate required"));
                    return;
                }

                List<Map<String, Object>> points = pointDao.getComputedPointsByFunctionId(functionId);
                double y = interpolateValue(points, x);

                Map<String, Object> result = Map.of(
                        "x", x,
                        "y", y,
                        "interpolated", true
                );
                mapper.writeValue(response.getWriter(), result);
                logger.info("SUCCESS: Computed f(" + x + ") = " + y + " for function " + functionId);

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

        logger.info("=== FUNCTION API PUT REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long functionId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: PUT /api/functions/" + functionId + " - Update function");

                Map<String, Object> existingFunction = functionDao.getFunctionById(functionId);
                if (existingFunction == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                    return;
                }

                Long ownerId = (Long) existingFunction.get("owner_id");
                if (!hasAccess(request, ownerId)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                    return;
                }

                String name = (String) body.get("functionName");
                Boolean isPublic = getBooleanFromObject(body.get("isPublic"));

                String newName = name != null ? name : (String) existingFunction.get("function_name");
                boolean newIsPublic = isPublic != null ? isPublic : (Boolean) existingFunction.get("is_public");

                functionDao.updateFunction(functionId, newName, (String) existingFunction.get("function_definition"), newIsPublic);

                mapper.writeValue(response.getWriter(), Map.of("message", "Function updated successfully"));
                logger.info("SUCCESS: Updated function ID " + functionId);

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

        logger.info("=== FUNCTION API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long functionId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: DELETE /api/functions/" + functionId + " - Delete function");

                Map<String, Object> existingFunction = functionDao.getFunctionById(functionId);
                if (existingFunction != null) {
                    Long ownerId = (Long) existingFunction.get("owner_id");
                    if (!hasAccess(request, ownerId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        mapper.writeValue(response.getWriter(), Map.of("error", "Access denied"));
                        return;
                    }

                    functionDao.deleteFunction(functionId);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    logger.info("SUCCESS: Deleted function ID " + functionId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
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

    private Map<String, Object> generateGraphData(Long functionId, Map<String, Object> functionData, List<Map<String, Object>> points) {
        List<Map<String, Object>> graphPoints = new ArrayList<>();
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;

        for (Map<String, Object> point : points) {
            double x = (Double) point.get("x_value");
            double y = (Double) point.get("y_value");

            Map<String, Object> graphPoint = new HashMap<>();
            graphPoint.put("x", x);
            graphPoint.put("y", y);
            graphPoints.add(graphPoint);

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        Map<String, Object> xRange = new HashMap<>();
        xRange.put("min", minX != Double.MAX_VALUE ? minX : -10);
        xRange.put("max", maxX != Double.MIN_VALUE ? maxX : 10);

        Map<String, Object> yRange = new HashMap<>();
        yRange.put("min", minY != Double.MAX_VALUE ? minY : -1);
        yRange.put("max", maxY != Double.MIN_VALUE ? maxY : 1);

        Map<String, Object> result = new HashMap<>();
        result.put("functionId", functionId);
        result.put("functionName", functionData != null ? functionData.get("function_name") : "Function " + functionId);
        result.put("points", graphPoints);
        result.put("xRange", xRange);
        result.put("yRange", yRange);

        return result;
    }

    private double computeMathFunction(String functionName, double x) {
        switch (functionName) {
            case "Квадратичная функция":
                return x * x;
            case "Тождественная функция":
                return x;
            case "Синус":
                return Math.sin(x);
            case "Косинус":
                return Math.cos(x);
            default:
                return x;
        }
    }

    private double interpolateValue(List<Map<String, Object>> points, double x) {
        if (points.isEmpty()) return 0;

        for (int i = 0; i < points.size() - 1; i++) {
            double x1 = (Double) points.get(i).get("x_value");
            double y1 = (Double) points.get(i).get("y_value");
            double x2 = (Double) points.get(i + 1).get("x_value");
            double y2 = (Double) points.get(i + 1).get("y_value");

            if (x >= x1 && x <= x2) {
                return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
            }
        }

        if (x < (Double) points.get(0).get("x_value")) {
            return (Double) points.get(0).get("y_value");
        } else {
            return (Double) points.get(points.size() - 1).get("y_value");
        }
    }
}