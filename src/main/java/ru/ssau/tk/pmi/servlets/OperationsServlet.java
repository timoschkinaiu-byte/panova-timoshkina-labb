package ru.ssau.tk.pmi.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import ru.ssau.tk.pmi.repository.manual.*;
import ru.ssau.tk.pmi.functions.*;
import ru.ssau.tk.pmi.functions.factory.*;
import ru.ssau.tk.pmi.operations.TabulatedFunctionOperationService;
import ru.ssau.tk.pmi.operations.DifferentialOperator;
import ru.ssau.tk.pmi.integration.MultiThreadIntegralSolver;

public class OperationsServlet extends BaseServlet {
    private static final Logger logger = Logger.getLogger(OperationsServlet.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

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
            mapper.writeValue(response.getWriter(), Map.of("error", "Invalid request: " + e.getMessage()));
        }

        logger.info("=== OPERATIONS API COMPLETED ===\n");
    }

    private void handleAddOperation(HttpServletRequest request, HttpServletResponse response,
                                    Map<String, Object> body) throws IOException, SQLException {
        Long function1Id = getLongFromObject(body.get("function1Id"));
        Long function2Id = getLongFromObject(body.get("function2Id"));
        String factoryType = (String) body.get("factoryType");
        if (factoryType == null) factoryType = "ARRAY";

        if (function1Id == null || function2Id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "function1Id and function2Id required"));
            return;
        }

        try (Connection connection = getConnection()) {
            // Проверка доступа к функциям
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            Map<String, Object> func1Data = functionDao.getFunctionById(function1Id);
            Map<String, Object> func2Data = functionDao.getFunctionById(function2Id);

            if (func1Data == null || func2Data == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "One or both functions not found"));
                return;
            }

            Long owner1Id = (Long) func1Data.get("owner_id");
            Long owner2Id = (Long) func2Data.get("owner_id");
            Boolean isPublic1 = (Boolean) func1Data.get("is_public");
            Boolean isPublic2 = (Boolean) func2Data.get("is_public");

            if ((!isPublic1 && !hasAccess(request, owner1Id)) || (!isPublic2 && !hasAccess(request, owner2Id))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to one or both functions"));
                return;
            }

            // Получаем точки функций
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            List<Map<String, Object>> points1 = pointDao.getComputedPointsByFunctionId(function1Id);
            List<Map<String, Object>> points2 = pointDao.getComputedPointsByFunctionId(function2Id);

            // Создаем TabulatedFunction из точек
            TabulatedFunction func1 = createTabulatedFunctionFromPoints(points1);
            TabulatedFunction func2 = createTabulatedFunctionFromPoints(points2);

            // Создаем фабрику и сервис операций
            TabulatedFunctionFactory factory = createFactory(factoryType);
            TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(factory);

            // Выполняем сложение
            TabulatedFunction resultFunction = service.add(func1, func2);

            // Сохраняем результат
            Long currentUserId = (Long) getAuthenticatedUser(request).get("user_id");
            String resultName = "Sum_" + function1Id + "_" + function2Id;
            Long resultFunctionId = saveTabulatedFunction(resultFunction, resultName, currentUserId, functionDao, pointDao);

            // Подготавливаем ответ
            Map<String, Object> result = new HashMap<>();
            result.put("functionId", resultFunctionId);
            result.put("functionName", resultName);
            result.put("functionType", "TABULATED");
            result.put("ownerId", currentUserId);
            result.put("isPublic", false);
            result.put("pointsCount", resultFunction.getCount());
            result.put("createdAt", new java.util.Date().toString());

            mapper.writeValue(response.getWriter(), result);
            logger.info("SUCCESS: Added functions " + function1Id + " and " + function2Id);
        }
    }

    private void handleSubtractOperation(HttpServletRequest request, HttpServletResponse response,
                                         Map<String, Object> body) throws IOException, SQLException {
        Long function1Id = getLongFromObject(body.get("function1Id"));
        Long function2Id = getLongFromObject(body.get("function2Id"));
        String factoryType = (String) body.get("factoryType");
        if (factoryType == null) factoryType = "ARRAY";

        if (function1Id == null || function2Id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "function1Id and function2Id required"));
            return;
        }

        try (Connection connection = getConnection()) {
            // Проверка доступа к функциям
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            Map<String, Object> func1Data = functionDao.getFunctionById(function1Id);
            Map<String, Object> func2Data = functionDao.getFunctionById(function2Id);

            if (func1Data == null || func2Data == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "One or both functions not found"));
                return;
            }

            Long owner1Id = (Long) func1Data.get("owner_id");
            Long owner2Id = (Long) func2Data.get("owner_id");
            Boolean isPublic1 = (Boolean) func1Data.get("is_public");
            Boolean isPublic2 = (Boolean) func2Data.get("is_public");

            if ((!isPublic1 && !hasAccess(request, owner1Id)) || (!isPublic2 && !hasAccess(request, owner2Id))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to one or both functions"));
                return;
            }

            // Получаем точки функций
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            List<Map<String, Object>> points1 = pointDao.getComputedPointsByFunctionId(function1Id);
            List<Map<String, Object>> points2 = pointDao.getComputedPointsByFunctionId(function2Id);

            // Создаем TabulatedFunction из точек
            TabulatedFunction func1 = createTabulatedFunctionFromPoints(points1);
            TabulatedFunction func2 = createTabulatedFunctionFromPoints(points2);

            // Создаем фабрику и сервис операций
            TabulatedFunctionFactory factory = createFactory(factoryType);
            TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(factory);

            // Выполняем вычитание
            TabulatedFunction resultFunction = service.subtraction(func1, func2);

            // Сохраняем результат
            Long currentUserId = (Long) getAuthenticatedUser(request).get("user_id");
            String resultName = "Difference_" + function1Id + "_" + function2Id;
            Long resultFunctionId = saveTabulatedFunction(resultFunction, resultName, currentUserId, functionDao, pointDao);

            // Подготавливаем ответ
            Map<String, Object> result = new HashMap<>();
            result.put("functionId", resultFunctionId);
            result.put("functionName", resultName);
            result.put("functionType", "TABULATED");
            result.put("ownerId", currentUserId);
            result.put("isPublic", false);
            result.put("pointsCount", resultFunction.getCount());
            result.put("createdAt", new java.util.Date().toString());

            mapper.writeValue(response.getWriter(), result);
            logger.info("SUCCESS: Subtracted functions " + function1Id + " and " + function2Id);
        }
    }

    private void handleMultiplyOperation(HttpServletRequest request, HttpServletResponse response,
                                         Map<String, Object> body) throws IOException, SQLException {
        Long function1Id = getLongFromObject(body.get("function1Id"));
        Long function2Id = getLongFromObject(body.get("function2Id"));
        String factoryType = (String) body.get("factoryType");
        if (factoryType == null) factoryType = "ARRAY";

        if (function1Id == null || function2Id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "function1Id and function2Id required"));
            return;
        }

        try (Connection connection = getConnection()) {
            // Проверка доступа к функциям
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            Map<String, Object> func1Data = functionDao.getFunctionById(function1Id);
            Map<String, Object> func2Data = functionDao.getFunctionById(function2Id);

            if (func1Data == null || func2Data == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "One or both functions not found"));
                return;
            }

            Long owner1Id = (Long) func1Data.get("owner_id");
            Long owner2Id = (Long) func2Data.get("owner_id");
            Boolean isPublic1 = (Boolean) func1Data.get("is_public");
            Boolean isPublic2 = (Boolean) func2Data.get("is_public");

            if ((!isPublic1 && !hasAccess(request, owner1Id)) || (!isPublic2 && !hasAccess(request, owner2Id))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to one or both functions"));
                return;
            }

            // Получаем точки функций
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            List<Map<String, Object>> points1 = pointDao.getComputedPointsByFunctionId(function1Id);
            List<Map<String, Object>> points2 = pointDao.getComputedPointsByFunctionId(function2Id);

            // Создаем TabulatedFunction из точек
            TabulatedFunction func1 = createTabulatedFunctionFromPoints(points1);
            TabulatedFunction func2 = createTabulatedFunctionFromPoints(points2);

            // Создаем фабрику и сервис операций
            TabulatedFunctionFactory factory = createFactory(factoryType);
            TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(factory);

            // Выполняем умножение
            TabulatedFunction resultFunction = service.multiplication(func1, func2);

            // Сохраняем результат
            Long currentUserId = (Long) getAuthenticatedUser(request).get("user_id");
            String resultName = "Product_" + function1Id + "_" + function2Id;
            Long resultFunctionId = saveTabulatedFunction(resultFunction, resultName, currentUserId, functionDao, pointDao);

            // Подготавливаем ответ
            Map<String, Object> result = new HashMap<>();
            result.put("functionId", resultFunctionId);
            result.put("functionName", resultName);
            result.put("functionType", "TABULATED");
            result.put("ownerId", currentUserId);
            result.put("isPublic", false);
            result.put("pointsCount", resultFunction.getCount());
            result.put("createdAt", new java.util.Date().toString());

            mapper.writeValue(response.getWriter(), result);
            logger.info("SUCCESS: Multiplied functions " + function1Id + " and " + function2Id);
        }
    }

    private void handleDivideOperation(HttpServletRequest request, HttpServletResponse response,
                                       Map<String, Object> body) throws IOException, SQLException {
        Long function1Id = getLongFromObject(body.get("function1Id"));
        Long function2Id = getLongFromObject(body.get("function2Id"));
        String factoryType = (String) body.get("factoryType");
        if (factoryType == null) factoryType = "ARRAY";

        if (function1Id == null || function2Id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "function1Id and function2Id required"));
            return;
        }

        try (Connection connection = getConnection()) {
            // Проверка доступа к функциям
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            Map<String, Object> func1Data = functionDao.getFunctionById(function1Id);
            Map<String, Object> func2Data = functionDao.getFunctionById(function2Id);

            if (func1Data == null || func2Data == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "One or both functions not found"));
                return;
            }

            Long owner1Id = (Long) func1Data.get("owner_id");
            Long owner2Id = (Long) func2Data.get("owner_id");
            Boolean isPublic1 = (Boolean) func1Data.get("is_public");
            Boolean isPublic2 = (Boolean) func2Data.get("is_public");

            if ((!isPublic1 && !hasAccess(request, owner1Id)) || (!isPublic2 && !hasAccess(request, owner2Id))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to one or both functions"));
                return;
            }

            // Получаем точки функций
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            List<Map<String, Object>> points1 = pointDao.getComputedPointsByFunctionId(function1Id);
            List<Map<String, Object>> points2 = pointDao.getComputedPointsByFunctionId(function2Id);

            // Создаем TabulatedFunction из точек
            TabulatedFunction func1 = createTabulatedFunctionFromPoints(points1);
            TabulatedFunction func2 = createTabulatedFunctionFromPoints(points2);

            // Проверка деления на ноль
            for (int i = 0; i < func2.getCount(); i++) {
                if (Math.abs(func2.getY(i)) < 0.000001) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    mapper.writeValue(response.getWriter(), Map.of("error", "Division by zero detected"));
                    return;
                }
            }

            // Создаем фабрику и сервис операций
            TabulatedFunctionFactory factory = createFactory(factoryType);
            TabulatedFunctionOperationService service = new TabulatedFunctionOperationService(factory);

            // Выполняем деление
            TabulatedFunction resultFunction = service.division(func1, func2);

            // Сохраняем результат
            Long currentUserId = (Long) getAuthenticatedUser(request).get("user_id");
            String resultName = "Quotient_" + function1Id + "_" + function2Id;
            Long resultFunctionId = saveTabulatedFunction(resultFunction, resultName, currentUserId, functionDao, pointDao);

            // Подготавливаем ответ
            Map<String, Object> result = new HashMap<>();
            result.put("functionId", resultFunctionId);
            result.put("functionName", resultName);
            result.put("functionType", "TABULATED");
            result.put("ownerId", currentUserId);
            result.put("isPublic", false);
            result.put("pointsCount", resultFunction.getCount());
            result.put("createdAt", new java.util.Date().toString());

            mapper.writeValue(response.getWriter(), result);
            logger.info("SUCCESS: Divided functions " + function1Id + " and " + function2Id);
        }
    }

    private void handleDifferentiateOperation(HttpServletRequest request, HttpServletResponse response,
                                              Map<String, Object> body) throws IOException, SQLException {
        Long functionId = getLongFromObject(body.get("functionId"));
        String factoryType = (String) body.get("factoryType");
        if (factoryType == null) factoryType = "ARRAY";

        if (functionId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "functionId required"));
            return;
        }

        try (Connection connection = getConnection()) {
            // Проверка доступа к функции
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            Map<String, Object> funcData = functionDao.getFunctionById(functionId);

            if (funcData == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                return;
            }

            Long ownerId = (Long) funcData.get("owner_id");
            Boolean isPublic = (Boolean) funcData.get("is_public");

            if (!isPublic && !hasAccess(request, ownerId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                return;
            }

            // Получаем точки функции
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            List<Map<String, Object>> points = pointDao.getComputedPointsByFunctionId(functionId);

            // Создаем TabulatedFunction из точек
            TabulatedFunction function = createTabulatedFunctionFromPoints(points);

            // Создаем дифференциальный оператор
            TabulatedFunctionFactory factory = createFactory(factoryType);
            DifferentialOperator<TabulatedFunction> differentialOperator = createDifferentialOperator(factory);

            // Выполняем дифференцирование
            TabulatedFunction resultFunction = differentialOperator.derive(function);

            // Сохраняем результат
            Long currentUserId = (Long) getAuthenticatedUser(request).get("user_id");
            String resultName = "Derivative_" + functionId;
            Long resultFunctionId = saveTabulatedFunction(resultFunction, resultName, currentUserId, functionDao, pointDao);

            // Подготавливаем ответ
            Map<String, Object> result = new HashMap<>();
            result.put("functionId", resultFunctionId);
            result.put("functionName", resultName);
            result.put("functionType", "TABULATED");
            result.put("ownerId", currentUserId);
            result.put("isPublic", false);
            result.put("pointsCount", resultFunction.getCount());
            result.put("createdAt", new java.util.Date().toString());

            mapper.writeValue(response.getWriter(), result);
            logger.info("SUCCESS: Differentiated function " + functionId);
        }
    }

    private void handleIntegrateOperation(HttpServletRequest request, HttpServletResponse response,
                                          Map<String, Object> body) throws IOException, SQLException {
        Long functionId = getLongFromObject(body.get("functionId"));
        Integer threadsCount = (Integer) body.get("threadsCount");

        if (functionId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(response.getWriter(), Map.of("error", "functionId required"));
            return;
        }

        if (threadsCount == null || threadsCount < 1) {
            threadsCount = 4; // Значение по умолчанию
        }

        try (Connection connection = getConnection()) {
            // Проверка доступа к функции
            FunctionDao functionDao = new JdbcFunctionDao(connection);
            Map<String, Object> funcData = functionDao.getFunctionById(functionId);

            if (funcData == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(response.getWriter(), Map.of("error", "Function not found"));
                return;
            }

            Long ownerId = (Long) funcData.get("owner_id");
            Boolean isPublic = (Boolean) funcData.get("is_public");

            if (!isPublic && !hasAccess(request, ownerId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                mapper.writeValue(response.getWriter(), Map.of("error", "Access denied to function"));
                return;
            }

            // Получаем точки функции
            ComputedPointDao pointDao = new JdbcComputedPointDao(connection);
            List<Map<String, Object>> points = pointDao.getComputedPointsByFunctionId(functionId);

            // Создаем TabulatedFunction из точек
            TabulatedFunction function = createTabulatedFunctionFromPoints(points);

            // Выполняем интегрирование
            long startTime = System.currentTimeMillis();
            MultiThreadIntegralSolver solver = new MultiThreadIntegralSolver(threadsCount);
            double integralResult = solver.computeIntegral(function);
            long computationTime = System.currentTimeMillis() - startTime;

            // Подготавливаем ответ
            Map<String, Object> result = new HashMap<>();
            result.put("result", integralResult);
            result.put("computationTime", computationTime);
            result.put("threadsCount", threadsCount);
            result.put("functionId", functionId);

            mapper.writeValue(response.getWriter(), result);
            logger.info("SUCCESS: Integrated function " + functionId + " with " + threadsCount + " threads, result: " + integralResult);
        }
    }

    // Вспомогательные методы

    private TabulatedFunction createTabulatedFunctionFromPoints(List<Map<String, Object>> points) {
        if (points == null || points.isEmpty()) {
            return new ArrayTabulatedFunction(new double[]{0, 1}, new double[]{0, 0});
        }

        // Сортируем точки по X
        points.sort((p1, p2) -> {
            double x1 = (Double) p1.get("x_value");
            double x2 = (Double) p2.get("x_value");
            return Double.compare(x1, x2);
        });

        double[] xValues = new double[points.size()];
        double[] yValues = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            Map<String, Object> point = points.get(i);
            xValues[i] = (Double) point.get("x_value");
            yValues[i] = (Double) point.get("y_value");
        }

        return new ArrayTabulatedFunction(xValues, yValues);
    }

    private TabulatedFunctionFactory createFactory(String factoryType) {
        if ("LINKED_LIST".equalsIgnoreCase(factoryType)) {
            return new LinkedListTabulatedFunctionFactory();
        } else {
            return new ArrayTabulatedFunctionFactory();
        }
    }

    private DifferentialOperator<TabulatedFunction> createDifferentialOperator(TabulatedFunctionFactory factory) {
        return new DifferentialOperator<TabulatedFunction>() {
            @Override
            public TabulatedFunction derive(TabulatedFunction function) {
                int count = function.getCount();
                double[] xValues = new double[count];
                double[] yValues = new double[count];

                // Простая численная производная
                for (int i = 0; i < count; i++) {
                    xValues[i] = function.getX(i);
                    if (i == 0) {
                        // Первая производная: forward difference
                        yValues[i] = (function.getY(i+1) - function.getY(i)) / (function.getX(i+1) - function.getX(i));
                    } else if (i == count - 1) {
                        // Последняя производная: backward difference
                        yValues[i] = (function.getY(i) - function.getY(i-1)) / (function.getX(i) - function.getX(i-1));
                    } else {
                        // Центральная разность
                        yValues[i] = (function.getY(i+1) - function.getY(i-1)) / (function.getX(i+1) - function.getX(i-1));
                    }
                }

                return factory.create(xValues, yValues);
            }
        };
    }

    private Long saveTabulatedFunction(TabulatedFunction function, String name, Long ownerId,
                                       FunctionDao functionDao, ComputedPointDao pointDao) {
        // Сохраняем функцию
        Long functionId = functionDao.insertFunction(name, "Operation result", ownerId, false);

        if (functionId != null) {
            // Сохраняем точки
            for (int i = 0; i < function.getCount(); i++) {
                pointDao.insertComputedPoint(functionId, function.getX(i), function.getY(i));
            }
        }

        return functionId;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Map<String, Object> user = getAuthenticatedUser(request);
        return user != null ? (Long) user.get("user_id") : null;
    }
}