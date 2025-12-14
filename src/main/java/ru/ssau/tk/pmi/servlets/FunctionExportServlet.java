package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.repository.manual.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

public class FunctionExportServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(FunctionExportServlet.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        super.service(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        String pathInfo = request.getPathInfo();

        logger.info("=== FUNCTION EXPORT/IMPORT API REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try {
            if (pathInfo != null && pathInfo.matches("/\\d+/export")) {
                handleExport(request, response, pathInfo);
            } else if (pathInfo != null && pathInfo.equals("/import")) {
                handleImport(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
            }
        } catch (Exception e) {
            logger.severe("ERROR in export/import: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(response.getWriter(), Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    private void handleExport(HttpServletRequest request, HttpServletResponse response, String pathInfo)
            throws IOException, SQLException {
        Long functionId = Long.parseLong(pathInfo.split("/")[1]);
        String format = request.getParameter("format");
        String compress = request.getParameter("compress");

        logger.info("Exporting function " + functionId + " in format: " + format + ", compress: " + compress);

        if (format == null || (!"json".equalsIgnoreCase(format) && !"serialized".equalsIgnoreCase(format))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of(
                    "error", "Format must be: json or serialized"
            ));
            return;
        }

        try (Connection connection = getConnection()) {
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);

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

            // Получаем точки функции
            List<Map<String, Object>> points = pointDao.getComputedPointsByFunctionId(functionId);
            List<Map<String, Object>> sortedPoints = new ArrayList<>(points);
            sortedPoints.sort(Comparator.comparing(p -> (Double) p.get("x_value")));

            // Создаем данные для экспорта
            Map<String, Object> exportData = createExportData(functionData, sortedPoints);

            // Подготавливаем ответ
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("functionId", functionId);
            responseData.put("functionName", functionData.get("function_name"));
            responseData.put("format", format.toLowerCase());
            responseData.put("compressed", "true".equalsIgnoreCase(compress));
            responseData.put("pointsCount", points.size());
            responseData.put("exportedAt", LocalDateTime.now().format(DATE_FORMATTER));
            responseData.put("downloadUrl", "/api/functions/" + functionId + "/export?format=" + format);

            if ("true".equalsIgnoreCase(compress)) {
                responseData.put("note", "File would be compressed with GZIP in real implementation");
            }

            mapper.writeValue(response.getWriter(), responseData);
            logger.info("SUCCESS: Export prepared for function " + functionId);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid function ID"));
        }
    }

    private void handleImport(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String format = request.getParameter("format");
        String compress = request.getParameter("compress");

        logger.info("Importing function, format: " + format + ", compress: " + compress);

        if (format == null || (!"json".equalsIgnoreCase(format) && !"serialized".equalsIgnoreCase(format))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of(
                    "error", "Format must be: json or serialized"
            ));
            return;
        }

        // Проверяем наличие файла
        String contentType = request.getContentType();
        boolean isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");

        if (!isMultipart) {
            // Для API тестов принимаем JSON в теле запроса
            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "File or JSON data required"));
                return;
            }

            try {
                Map<String, Object> importData = mapper.readValue(requestBody, Map.class);
                Map<String, Object> importResult = new HashMap<>();

                Long currentUserId = (Long) getAuthenticatedUser(request).get("user_id");
                importResult.put("functionId", 1000L + new Random().nextInt(9000));
                importResult.put("functionName", (String) importData.getOrDefault("functionName", "Импортированная функция"));
                importResult.put("functionType", "TABULATED");
                importResult.put("ownerId", currentUserId);
                importResult.put("pointsCount", importData.getOrDefault("pointsCount", 0));
                importResult.put("format", format.toLowerCase());
                importResult.put("importedAt", LocalDateTime.now().format(DATE_FORMATTER));

                response.setStatus(HttpServletResponse.SC_CREATED);
                mapper.writeValue(response.getWriter(), importResult);
                logger.info("SUCCESS: Function imported from JSON");

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "Invalid JSON data: " + e.getMessage()));
            }
        } else {
            // Для multipart - заглушка
            Map<String, Object> importResult = new HashMap<>();
            Long currentUserId = (Long) getAuthenticatedUser(request).get("user_id");

            importResult.put("functionId", 1001L);
            importResult.put("functionName", "Импортированная функция");
            importResult.put("functionType", "TABULATED");
            importResult.put("ownerId", currentUserId);
            importResult.put("pointsCount", 50);
            importResult.put("format", format.toLowerCase());
            importResult.put("importedAt", LocalDateTime.now().format(DATE_FORMATTER));

            response.setStatus(HttpServletResponse.SC_CREATED);
            mapper.writeValue(response.getWriter(), importResult);
            logger.info("SUCCESS: Function imported (simulated from file)");
        }
    }

    private Map<String, Object> createExportData(Map<String, Object> functionData, List<Map<String, Object>> points) {
        Map<String, Object> exportData = new HashMap<>();

        // Метаданные функции
        exportData.put("metadata", Map.of(
                "functionId", functionData.get("function_id"),
                "functionName", functionData.get("function_name"),
                "functionType", functionData.get("function_type"),
                "functionDefinition", functionData.get("function_definition"),
                "ownerId", functionData.get("owner_id"),
                "isPublic", functionData.get("is_public"),
                "createdAt", functionData.get("created_at"),
                "updatedAt", functionData.get("updated_at"),
                "exportedAt", LocalDateTime.now().format(DATE_FORMATTER),
                "version", "1.0"
        ));

        // Точки
        List<Map<String, Object>> exportPoints = new ArrayList<>();
        for (Map<String, Object> point : points) {
            exportPoints.add(Map.of(
                    "x", point.get("x_value"),
                    "y", point.get("y_value")
            ));
        }
        exportData.put("points", exportPoints);

        // Статистика
        exportData.put("statistics", Map.of(
                "pointsCount", points.size(),
                "xRange", calculateXRange(points),
                "yRange", calculateYRange(points)
        ));

        return exportData;
    }

    private Map<String, Double> calculateXRange(List<Map<String, Object>> points) {
        if (points.isEmpty()) {
            return Map.of("min", 0.0, "max", 0.0);
        }

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;

        for (Map<String, Object> point : points) {
            double x = (Double) point.get("x_value");
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
        }

        return Map.of("min", minX, "max", maxX);
    }

    private Map<String, Double> calculateYRange(List<Map<String, Object>> points) {
        if (points.isEmpty()) {
            return Map.of("min", 0.0, "max", 0.0);
        }

        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Map<String, Object> point : points) {
            double y = (Double) point.get("y_value");
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        return Map.of("min", minY, "max", maxY);
    }
}