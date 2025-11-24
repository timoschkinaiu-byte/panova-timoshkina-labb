package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.dto.ComputedPointDto;
import ru.ssau.tk.pmi.dto.DtoMapper;
import ru.ssau.tk.pmi.repository.manual.ComputedPointDao;
import ru.ssau.tk.pmi.repository.manual.JdbcComputedPointDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;


public class ComputedPointServlet extends BaseServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        super.service(request, response);
    }
    private static final Logger logger = Logger.getLogger(ComputedPointServlet.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== COMPUTED POINTS API GET REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);
        logger.info("Query Params: " + request.getQueryString());

        try (Connection connection = getConnection()) {
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);

            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/points - поиск точек
                logger.info("API: GET /api/points - Search points with filters");

                String functionId = request.getParameter("functionId");
                String xFrom = request.getParameter("xFrom");
                String xTo = request.getParameter("xTo");

                List<Map<String, Object>> pointsData;

                if (functionId != null && !functionId.isEmpty()) {
                    Long funcId = Long.parseLong(functionId);
                    pointsData = pointDao.getComputedPointsByFunctionId(funcId);
                    logger.info("Filtered by functionId: " + funcId);
                } else {
                    pointsData = pointDao.getAllComputedPoints();
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
                // GET /api/points/search - поиск по координатам
                logger.info("API: GET /api/points/search - Search points by coordinates");

                String x = request.getParameter("x");
                String y = request.getParameter("y");
                String functionId = request.getParameter("functionId");

                List<Map<String, Object>> allPoints = pointDao.getAllComputedPoints();
                List<Map<String, Object>> searchResults = new ArrayList<>();

                for (Map<String, Object> point : allPoints) {
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

                    if (functionId != null && !functionId.isEmpty()) {
                        Long funcId = Long.parseLong(functionId);
                        Long pointFuncId = (Long) point.get("function_id");
                        if (!pointFuncId.equals(funcId)) match = false;
                    }

                    if (match) {
                        searchResults.add(point);
                    }
                }

                mapper.writeValue(response.getWriter(), searchResults);
                logger.info("SUCCESS: Found " + searchResults.size() + " points by search");

            } else {
                // GET /api/points/{id} - точка по ID
                Long pointId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: GET /api/points/" + pointId + " - Get point by ID");

                Map<String, Object> pointData = pointDao.getComputedPointById(pointId);
                ComputedPointDto point = DtoMapper.mapToComputedPointDto(pointData);

                if (point != null) {
                    mapper.writeValue(response.getWriter(), point);
                    logger.info("SUCCESS: Returned point: " + pointId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Point not found"));
                    logger.warning("NOT FOUND: Point ID " + pointId + " not found");
                }
            }
        } catch (Exception e) {
            logger.severe("ERROR in GET: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== COMPUTED POINTS API GET COMPLETED ===\n");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        logger.info("=== COMPUTED POINTS API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());

        try (Connection connection = getConnection()) {
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
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
                logger.warning("VALIDATION: Missing required parameters");
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
                logger.severe("ERROR: Failed to insert point into database");
            }

        } catch (Exception e) {
            logger.severe("ERROR in POST: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== COMPUTED POINTS API POST COMPLETED ===\n");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== COMPUTED POINTS API PUT REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
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
                    logger.warning("NOT FOUND: Point ID " + pointId + " not found for update");
                    return;
                }

                Double yValue = getDoubleFromObject(body.get("yValue"));
                if (yValue == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "yValue is required"));
                    logger.warning("VALIDATION: Missing yValue parameter");
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

        logger.info("=== COMPUTED POINTS API PUT COMPLETED ===\n");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        logger.info("=== COMPUTED POINTS API DELETE REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        try (Connection connection = getConnection()) {
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);

            if (pathInfo != null && !pathInfo.equals("/")) {
                Long pointId = Long.parseLong(pathInfo.substring(1));
                logger.info("API: DELETE /api/points/" + pointId + " - Delete point");

                Map<String, Object> existingPoint = pointDao.getComputedPointById(pointId);
                if (existingPoint != null) {
                    pointDao.deleteComputedPoint(pointId);
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    logger.info("SUCCESS: Deleted point: " + pointId);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Point not found"));
                    logger.warning("NOT FOUND: Point ID " + pointId + " not found for deletion");
                }
            }
        } catch (Exception e) {
            logger.severe("ERROR in DELETE: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== COMPUTED POINTS API DELETE COMPLETED ===\n");
    }
}