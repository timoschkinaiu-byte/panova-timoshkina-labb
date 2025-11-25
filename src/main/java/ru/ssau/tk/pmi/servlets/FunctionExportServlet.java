package ru.ssau.tk.pmi.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class FunctionExportServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(FunctionExportServlet.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

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

        logger.info("=== FUNCTION EXPORT API REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        if (pathInfo != null && pathInfo.matches("/\\d+/export")) {
            Long functionId = Long.parseLong(pathInfo.split("/")[1]);
            String format = request.getParameter("format");

            logger.info("API: POST /api/functions/" + functionId + "/export - Export function");

            if (format == null || !format.equals("json")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "Only JSON format supported"));
                return;
            }

            // Заглушка для экспорта
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("functionId", functionId);
            exportData.put("functionName", "Exported Function " + functionId);
            exportData.put("format", "json");
            exportData.put("exportedAt", new Date().toString());

            mapper.writeValue(response.getWriter(), exportData);
            logger.info("SUCCESS: Exported function " + functionId + " as JSON");

        } else if (pathInfo != null && pathInfo.equals("/import")) {
            logger.info("API: POST /api/functions/import - Import function");

            String format = request.getParameter("format");
            if (format == null || !format.equals("json")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(response.getWriter(), Map.of("error", "Only JSON format supported"));
                return;
            }

            Map<String, Object> importResult = new HashMap<>();
            importResult.put("functionId", 9999L);
            importResult.put("functionName", "Imported Function");
            importResult.put("message", "Function imported successfully");

            response.setStatus(HttpServletResponse.SC_CREATED);
            mapper.writeValue(response.getWriter(), importResult);
            logger.info("SUCCESS: Imported function");

        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            mapper.writeValue(response.getWriter(), Map.of("error", "Endpoint not found"));
        }
    }
}

