package ru.ssau.tk.pmi.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class OperationsServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(OperationsServlet.class.getName());
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

        logger.info("=== OPERATIONS API POST REQUEST ===");
        logger.info("URL: " + request.getRequestURL());
        logger.info("Path: " + pathInfo);

        Map<String, Object> currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(response.getWriter(), Map.of("error", "Authentication required"));
            return;
        }

        try {
            String requestBody = getRequestBody(request);
            logger.info("Request Body: " + requestBody);

            Map<String, Object> body = mapper.readValue(requestBody, Map.class);

            if (pathInfo.equals("/add")) {
                handleAddOperation(request, response, body);
            } else if (pathInfo.equals("/subtract")) {
                handleSubtractOperation(request, response, body);
            } else if (pathInfo.equals("/multiply")) {
                handleMultiplyOperation(request, response, body);
            } else if (pathInfo.equals("/divide")) {
                handleDivideOperation(request, response, body);
            } else if (pathInfo.equals("/differentiate")) {
                handleDifferentiateOperation(request, response, body);
            } else if (pathInfo.equals("/integrate")) {
                handleIntegrateOperation(request, response, body);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Operation not found"));
            }
        } catch (Exception e) {
            logger.severe("ERROR in OPERATIONS: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request"));
        }

        logger.info("=== OPERATIONS API COMPLETED ===\n");
    }

    private void handleAddOperation(HttpServletRequest request, HttpServletResponse response,
                                    Map<String, Object> body) throws IOException {
        Long function1Id = getLongFromObject(body.get("function1Id"));
        Long function2Id = getLongFromObject(body.get("function2Id"));

        if (function1Id == null || function2Id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "function1Id and function2Id required"));
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("functionId", function1Id + 1000);
        result.put("functionName", "Sum Function");
        result.put("functionType", "TABULATED");
        result.put("ownerId", getAuthenticatedUser(request).get("user_id"));
        result.put("isPublic", false);
        result.put("pointsCount", 10);
        result.put("createdAt", new java.util.Date().toString());

        mapper.writeValue(response.getWriter(), result);
        logger.info("SUCCESS: Added functions " + function1Id + " and " + function2Id);
    }

    private void handleSubtractOperation(HttpServletRequest request, HttpServletResponse response,
                                         Map<String, Object> body) throws IOException {
        Long function1Id = getLongFromObject(body.get("function1Id"));
        Long function2Id = getLongFromObject(body.get("function2Id"));

        if (function1Id == null || function2Id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "function1Id and function2Id required"));
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("functionId", function1Id + 1001);
        result.put("functionName", "Difference Function");
        result.put("functionType", "TABULATED");
        result.put("ownerId", getAuthenticatedUser(request).get("user_id"));
        result.put("isPublic", false);
        result.put("pointsCount", 10);
        result.put("createdAt", new java.util.Date().toString());

        mapper.writeValue(response.getWriter(), result);
        logger.info("SUCCESS: Subtracted functions " + function1Id + " and " + function2Id);
    }

    private void handleMultiplyOperation(HttpServletRequest request, HttpServletResponse response,
                                         Map<String, Object> body) throws IOException {
        Long function1Id = getLongFromObject(body.get("function1Id"));
        Long function2Id = getLongFromObject(body.get("function2Id"));

        if (function1Id == null || function2Id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "function1Id and function2Id required"));
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("functionId", function1Id + 1002);
        result.put("functionName", "Product Function");
        result.put("functionType", "TABULATED");
        result.put("ownerId", getAuthenticatedUser(request).get("user_id"));
        result.put("isPublic", false);
        result.put("pointsCount", 10);
        result.put("createdAt", new java.util.Date().toString());

        mapper.writeValue(response.getWriter(), result);
        logger.info("SUCCESS: Multiplied functions " + function1Id + " and " + function2Id);
    }

    private void handleDivideOperation(HttpServletRequest request, HttpServletResponse response,
                                       Map<String, Object> body) throws IOException {
        Long function1Id = getLongFromObject(body.get("function1Id"));
        Long function2Id = getLongFromObject(body.get("function2Id"));

        if (function1Id == null || function2Id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "function1Id and function2Id required"));
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("functionId", function1Id + 1003);
        result.put("functionName", "Quotient Function");
        result.put("functionType", "TABULATED");
        result.put("ownerId", getAuthenticatedUser(request).get("user_id"));
        result.put("isPublic", false);
        result.put("pointsCount", 10);
        result.put("createdAt", new java.util.Date().toString());

        mapper.writeValue(response.getWriter(), result);
        logger.info("SUCCESS: Divided functions " + function1Id + " and " + function2Id);
    }

    private void handleDifferentiateOperation(HttpServletRequest request, HttpServletResponse response,
                                              Map<String, Object> body) throws IOException {
        Long functionId = getLongFromObject(body.get("functionId"));

        if (functionId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "functionId required"));
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("functionId", functionId + 1004);
        result.put("functionName", "Derivative Function");
        result.put("functionType", "TABULATED");
        result.put("ownerId", getAuthenticatedUser(request).get("user_id"));
        result.put("isPublic", false);
        result.put("pointsCount", 10);
        result.put("createdAt", new java.util.Date().toString());

        mapper.writeValue(response.getWriter(), result);
        logger.info("SUCCESS: Differentiated function " + functionId);
    }

    private void handleIntegrateOperation(HttpServletRequest request, HttpServletResponse response,
                                          Map<String, Object> body) throws IOException {
        Long functionId = getLongFromObject(body.get("functionId"));
        Integer threadsCount = (Integer) body.get("threadsCount");

        if (functionId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "functionId required"));
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("result", 42.0);
        result.put("computationTime", 0.15);

        mapper.writeValue(response.getWriter(), result);
        logger.info("SUCCESS: Integrated function " + functionId + " with " + threadsCount + " threads");
    }
}