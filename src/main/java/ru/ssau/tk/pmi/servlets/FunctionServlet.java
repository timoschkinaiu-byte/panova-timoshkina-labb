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
        logger.info("Query Params: " + request.getQueryString());

        try (Connection connection = getConnection()) {
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                logger.info("API: GET /api/functions - Search functions");

                // Получаем все функции из БД
                List<Map<String, Object>> functionsData = functionDao.getAllFunctions();
                List<Map<String, Object>> functionList = new ArrayList<>();

                for (Map<String, Object> functionData : functionsData) {
                    Map<String, Object> functionMap = new HashMap<>();
                    functionMap.put("functionId", functionData.get("function_id"));
                    functionMap.put("functionName", functionData.get("function_name"));
                    functionMap.put("functionType", "TABULATED"); // По умолчанию
                    functionMap.put("ownerId", functionData.get("owner_id"));
                    functionMap.put("isPublic", functionData.get("is_public"));
                    functionMap.put("pointsCount", pointDao.getComputedPointsByFunctionId((Long)functionData.get("function_id")).size());
                    functionMap.put("createdAt", new java.util.Date().toString());
                    functionList.add(functionMap);
                }

                // Применяем фильтры
                String search = request.getParameter("search");
                String type = request.getParameter("type");
                String ownerId = request.getParameter("ownerId");
                String isPublic = request.getParameter("isPublic");

                if (search != null && !search.isEmpty()) {
                    functionList.removeIf(func ->
                            !((String)func.get("functionName")).toLowerCase().contains(search.toLowerCase()));
                    logger.info("Applied search filter: " + search);
                }

                if (type != null && !type.isEmpty()) {
                    functionList.removeIf(func -> !func.get("functionType").equals(type));
                    logger.info("Applied type filter: " + type);
                }

                if (ownerId != null && !ownerId.isEmpty()) {
                    Long ownerIdLong = Long.parseLong(ownerId);
                    functionList.removeIf(func -> !func.get("ownerId").equals(ownerIdLong));
                    logger.info("Applied owner filter: " + ownerId);
                }

                if (isPublic != null && !isPublic.isEmpty()) {
                    boolean isPublicBool = Boolean.parseBoolean(isPublic);
                    functionList.removeIf(func -> !func.get("isPublic").equals(isPublicBool));
                    logger.info("Applied public filter: " + isPublic);
                }

                mapper.writeValue(response.getWriter(), functionList);
                logger.info("SUCCESS: Returned " + functionList.size() + " functions");

            } else if (pathInfo.matches("/\\d+/points")) {
                Long functionId = Long.parseLong(pathInfo.split("/")[1]);
                logger.info("API: GET /api/functions/" + functionId + "/points - Get function points");

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
                List<Map<String, Object>> pointsData = pointDao.getComputedPointsByFunctionId(functionId);

                Map<String, Object> graphData = generateGraphData(functionId, functionData, pointsData);
                mapper.writeValue(response.getWriter(), graphData);
                logger.info("SUCCESS: Generated graph data for function " + functionId);

            } else if (pathInfo.matches("/\\d+")) {
                Long functionId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/functions/" + functionId + " - Get function by ID");

                Map<String, Object> functionData = functionDao.getFunctionById(functionId);
                if (functionData != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("functionId", functionData.get("function_id"));
                    responseData.put("functionName", functionData.get("function_name"));
                    responseData.put("functionType", "TABULATED");
                    responseData.put("ownerId", functionData.get("owner_id"));
                    responseData.put("isPublic", functionData.get("is_public"));
                    responseData.put("pointsCount", pointDao.getComputedPointsByFunctionId(functionId).size());
                    responseData.put("createdAt", new java.util.Date().toString());

                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Returned function ID " + functionId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                    logger.warning("NOT FOUND: Function ID " + functionId + " not found");
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
                logger.warning("NOT FOUND: Invalid endpoint " + pathInfo);
            }
        } catch (Exception e) {
            logger.severe("ERROR in GET: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== FUNCTION API GET COMPLETED ===\n");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== FUNCTION API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

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

                Long functionId = functionDao.insertFunction(functionName, functionDefinition,
                        ownerId != null ? ownerId : 1L, isPublic != null ? isPublic : false);

                if (functionId != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("functionId", functionId);
                    responseData.put("functionName", functionName);
                    responseData.put("functionType", "TABULATED");
                    responseData.put("ownerId", ownerId != null ? ownerId : 1L);
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
                String factoryType = request.getParameter("factoryType");

                if (name == null || xValues == null || yValues == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Name, xValues and yValues required"));
                    return;
                }

                // Создаем функцию
                String definition = "Array function with " + xValues.size() + " points (factory: " + factoryType + ")";
                Long functionId = functionDao.insertFunction(name, definition, 1L, true);

                // Сохраняем точки
                if (functionId != null) {
                    for (int i = 0; i < xValues.size(); i++) {
                        pointDao.insertComputedPoint(functionId, xValues.get(i), yValues.get(i));
                    }

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("functionId", functionId);
                    responseData.put("functionName", name);
                    responseData.put("functionType", "TABULATED");
                    responseData.put("ownerId", 1L);
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
                String factoryType = request.getParameter("factoryType");

                if (name == null || sourceFunctionName == null || leftX == null || rightX == null || pointsCount == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "All parameters required"));
                    return;
                }

                // Создаем функцию
                String definition = "Math function: " + sourceFunctionName + " from " + leftX + " to " + rightX;
                Long functionId = functionDao.insertFunction(name, definition, 1L, true);

                // Генерируем точки для математической функции
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
                    responseData.put("ownerId", 1L);
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
                String factoryType = request.getParameter("factoryType");

                if (name == null || outerFunctionName == null || innerFunctionName == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "All parameters required"));
                    return;
                }

                // Создаем композитную функцию
                String definition = "Composite: " + outerFunctionName + "(" + innerFunctionName + "(x))";
                Long functionId = functionDao.insertFunction(name, definition, 1L, true);

                // Генерируем точки для композитной функции
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
                    responseData.put("ownerId", 1L);
                    responseData.put("isPublic", true);
                    responseData.put("pointsCount", 41); // от -10 до 10 с шагом 0.5
                    responseData.put("createdAt", new java.util.Date().toString());

                    response.setStatus(HttpServletResponse.SC_CREATED);
                    mapper.writeValue(response.getWriter(), responseData);
                    logger.info("SUCCESS: Created composite function '" + name + "'");
                }

            } else if (pathInfo.matches("/\\d+/compute")) {
                Long functionId = Long.parseLong(pathInfo.split("/")[1]);
                logger.info("API: POST /api/functions/" + functionId + "/compute - Compute function value");

                Double x = getDoubleFromObject(body.get("x"));

                if (x == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "X coordinate required"));
                    return;
                }

                // Получаем точки функции и интерполируем
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
                logger.warning("NOT FOUND: Invalid endpoint " + pathInfo);
            }
        } catch (Exception e) {
            logger.severe("ERROR in POST: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== FUNCTION API POST COMPLETED ===\n");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== FUNCTION API PUT REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

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

        logger.info("=== FUNCTION API PUT COMPLETED ===\n");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        logger.info("=== FUNCTION API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            FunctionDao functionDao = new JdbcFunctionDao(connection);

            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long functionId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: DELETE /api/functions/" + functionId + " - Delete function");

                Map<String, Object> existingFunction = functionDao.getFunctionById(functionId);
                if (existingFunction != null) {
                    functionDao.deleteFunction(functionId);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    logger.info("SUCCESS: Deleted function ID " + functionId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                    logger.warning("NOT FOUND: Function ID " + functionId + " not found for deletion");
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

        logger.info("=== FUNCTION API DELETE COMPLETED ===\n");
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
                return x; // По умолчанию тождественная
        }
    }

    private double interpolateValue(List<Map<String, Object>> points, double x) {
        if (points.isEmpty()) return 0;

        // Простая линейная интерполяция
        for (int i = 0; i < points.size() - 1; i++) {
            double x1 = (Double) points.get(i).get("x_value");
            double y1 = (Double) points.get(i).get("y_value");
            double x2 = (Double) points.get(i + 1).get("x_value");
            double y2 = (Double) points.get(i + 1).get("y_value");

            if (x >= x1 && x <= x2) {
                return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
            }
        }

        // Если x вне диапазона, возвращаем ближайшее значение
        if (x < (Double) points.get(0).get("x_value")) {
            return (Double) points.get(0).get("y_value");
        } else {
            return (Double) points.get(points.size() - 1).get("y_value");
        }
    }
}