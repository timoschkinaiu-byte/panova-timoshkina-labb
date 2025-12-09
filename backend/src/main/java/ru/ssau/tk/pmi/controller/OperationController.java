package ru.ssau.tk.pmi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.FunctionDTO;
import ru.ssau.tk.pmi.dto.OperationDTO;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.exceptions.FunctionNotFoundException;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.operations.TabulatedFunctionOperationService;
import ru.ssau.tk.pmi.operations.DifferentialOperator;
import ru.ssau.tk.pmi.integration.MultiThreadIntegralSolver;
import ru.ssau.tk.pmi.repository.MathFunctionRepository;
import ru.ssau.tk.pmi.repository.UserRepository;
import ru.ssau.tk.pmi.service.SecurityService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private static final Logger logger = LoggerFactory.getLogger(OperationController.class);
    private final MathFunctionRepository functionRepository;
    private final UserRepository userRepository;
    private final TabulatedFunctionOperationService tabulatedOperationService;
    private final SecurityService securityService;

    public OperationController(MathFunctionRepository functionRepository,
                               UserRepository userRepository,
                               TabulatedFunctionOperationService tabulatedOperationService,
                               SecurityService securityService) {
        this.functionRepository = functionRepository;
        this.userRepository = userRepository;
        this.tabulatedOperationService = tabulatedOperationService;
        this.securityService = securityService;
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> addFunctions(@RequestBody OperationDTO.BinaryOperationRequest request) {
        logger.info("Сложение функций: {} + {}", request.getFunction1Id(), request.getFunction2Id());

        try {
            validateBinaryOperationRequest(request);

            securityService.checkFunctionAccess(request.getFunction1Id());
            securityService.checkFunctionAccess(request.getFunction2Id());

            TabulatedFunction function1 = getFunctionFromDb(request.getFunction1Id());
            TabulatedFunction function2 = getFunctionFromDb(request.getFunction2Id());

            TabulatedFunction result = tabulatedOperationService.add(function1, function2);
            MathFunction savedFunction = saveResultToDb(result, "Сумма_" + request.getFunction1Id() + "_" + request.getFunction2Id());

            FunctionDTO.Response response = convertToResponse(savedFunction);
            logger.info("Сложение выполнено. Результат: {}", savedFunction.getFunctionId());
            return ResponseEntity.ok(response);

        } catch (FunctionNotFoundException e) {
            logger.warn("Функция не найдена: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка сложения: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/subtract")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> subtractFunctions(@RequestBody OperationDTO.BinaryOperationRequest request) {
        logger.info("Вычитание функций: {} - {}", request.getFunction1Id(), request.getFunction2Id());

        try {
            validateBinaryOperationRequest(request);

            securityService.checkFunctionAccess(request.getFunction1Id());
            securityService.checkFunctionAccess(request.getFunction2Id());

            TabulatedFunction function1 = getFunctionFromDb(request.getFunction1Id());
            TabulatedFunction function2 = getFunctionFromDb(request.getFunction2Id());

            TabulatedFunction result = tabulatedOperationService.subtraction(function1, function2);
            MathFunction savedFunction = saveResultToDb(result, "Разность_" + request.getFunction1Id() + "_" + request.getFunction2Id());

            return ResponseEntity.ok(convertToResponse(savedFunction));

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка вычитания: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/multiply")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> multiplyFunctions(@RequestBody OperationDTO.BinaryOperationRequest request) {
        logger.info("Умножение функций: {} * {}", request.getFunction1Id(), request.getFunction2Id());

        try {
            validateBinaryOperationRequest(request);

            securityService.checkFunctionAccess(request.getFunction1Id());
            securityService.checkFunctionAccess(request.getFunction2Id());

            TabulatedFunction function1 = getFunctionFromDb(request.getFunction1Id());
            TabulatedFunction function2 = getFunctionFromDb(request.getFunction2Id());

            TabulatedFunction result = tabulatedOperationService.multiplication(function1, function2);
            MathFunction savedFunction = saveResultToDb(result, "Произведение_" + request.getFunction1Id() + "_" + request.getFunction2Id());

            return ResponseEntity.ok(convertToResponse(savedFunction));

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка умножения: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/divide")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> divideFunctions(@RequestBody OperationDTO.BinaryOperationRequest request) {
        logger.info("Деление функций: {} / {}", request.getFunction1Id(), request.getFunction2Id());

        try {
            validateBinaryOperationRequest(request);

            securityService.checkFunctionAccess(request.getFunction1Id());
            securityService.checkFunctionAccess(request.getFunction2Id());

            TabulatedFunction function1 = getFunctionFromDb(request.getFunction1Id());
            TabulatedFunction function2 = getFunctionFromDb(request.getFunction2Id());

            TabulatedFunction result = tabulatedOperationService.division(function1, function2);
            MathFunction savedFunction = saveResultToDb(result, "Частное_" + request.getFunction1Id() + "_" + request.getFunction2Id());

            return ResponseEntity.ok(convertToResponse(savedFunction));

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ArithmeticException e) {
            logger.warn("Деление на ноль: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Ошибка деления: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/differentiate")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> differentiateFunction(@RequestBody OperationDTO.UnaryOperationRequest request) {
        logger.info("Дифференцирование функции: {}", request.getFunctionId());

        try {
            validateUnaryOperationRequest(request);

            securityService.checkFunctionAccess(request.getFunctionId());

            TabulatedFunction function = getFunctionFromDb(request.getFunctionId());
            DifferentialOperator<TabulatedFunction> differentialOperator = createDifferentialOperator();

            TabulatedFunction result = differentialOperator.derive(function);
            MathFunction savedFunction = saveResultToDb(result, "Производная_" + request.getFunctionId());

            return ResponseEntity.ok(convertToResponse(savedFunction));

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка дифференцирования: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/integrate")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OperationDTO.IntegrateResponse> integrateFunction(@RequestBody OperationDTO.IntegrateRequest request) {
        logger.info("Интегрирование функции: {}, потоков: {}", request.getFunctionId(), request.getThreadsCount());

        try {
            validateIntegrateRequest(request);

            securityService.checkFunctionAccess(request.getFunctionId());

            TabulatedFunction function = getFunctionFromDb(request.getFunctionId());

            long startTime = System.currentTimeMillis();
            MultiThreadIntegralSolver solver = new MultiThreadIntegralSolver(request.getThreadsCount());
            double result = solver.computeIntegral(function);
            long computationTime = System.currentTimeMillis() - startTime;

            OperationDTO.IntegrateResponse response = new OperationDTO.IntegrateResponse();
            response.setResult(result);
            response.setComputationTime(computationTime);

            logger.info("Интеграл вычислен: {}, время: {} мс", result, computationTime);
            return ResponseEntity.ok(response);

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка интегрирования: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private TabulatedFunction getFunctionFromDb(Long functionId) {
        MathFunction mathFunction = functionRepository.findById(functionId)
                .orElseThrow(() -> new FunctionNotFoundException("Функция с ID " + functionId + " не найдена"));

        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        mathFunction.getComputedPoints().forEach(point -> {
            xValues.add(point.getXValue());
            yValues.add(point.getYValue());
        });

        return new ArrayTabulatedFunction(
                xValues.stream().mapToDouble(Double::doubleValue).toArray(),
                yValues.stream().mapToDouble(Double::doubleValue).toArray()
        );
    }

    private MathFunction saveResultToDb(TabulatedFunction tabulatedFunction, String name) {
        User currentUser = securityService.getCurrentUser();

        MathFunction mathFunction = new MathFunction();
        mathFunction.setFunctionName(name);
        mathFunction.setFunctionDefinition("Табулированная функция");
        mathFunction.setFunctionType("TABULATED");
        mathFunction.setOwner(currentUser);
        mathFunction.setIsPublic(false);
        mathFunction.setCreatedAt(LocalDateTime.now());
        mathFunction.setUpdatedAt(LocalDateTime.now());

        List<ru.ssau.tk.pmi.entity.ComputedPoint> points = new ArrayList<>();
        for (int i = 0; i < tabulatedFunction.getCount(); i++) {
            ru.ssau.tk.pmi.entity.ComputedPoint point = new ru.ssau.tk.pmi.entity.ComputedPoint();
            point.setXValue(tabulatedFunction.getX(i));
            point.setYValue(tabulatedFunction.getY(i));
            point.setFunction(mathFunction);
            points.add(point);
        }
        mathFunction.setComputedPoints(points);

        return functionRepository.save(mathFunction);
    }

    private DifferentialOperator<TabulatedFunction> createDifferentialOperator() {
        return function -> {
            int count = function.getCount();
            double[] xValues = new double[count];
            double[] yValues = new double[count];
            double step = 0.001;

            for (int i = 0; i < count; i++) {
                xValues[i] = function.getX(i);
                double x = xValues[i];
                yValues[i] = (function.apply(x) - function.apply(x - step)) / step;
            }

            return new ArrayTabulatedFunction(xValues, yValues);
        };
    }

    private void validateBinaryOperationRequest(OperationDTO.BinaryOperationRequest request) {
        if (request.getFunction1Id() == null || request.getFunction2Id() == null) {
            throw new IllegalArgumentException("ID функций не могут быть пустыми");
        }
    }

    private void validateUnaryOperationRequest(OperationDTO.UnaryOperationRequest request) {
        if (request.getFunctionId() == null) {
            throw new IllegalArgumentException("ID функции не может быть пустым");
        }
    }

    private void validateIntegrateRequest(OperationDTO.IntegrateRequest request) {
        if (request.getFunctionId() == null) {
            throw new IllegalArgumentException("ID функции не может быть пустым");
        }
        if (request.getThreadsCount() == null || request.getThreadsCount() < 1) {
            throw new IllegalArgumentException("Количество потоков должно быть положительным");
        }
    }

    private FunctionDTO.Response convertToResponse(MathFunction function) {
        FunctionDTO.Response response = new FunctionDTO.Response();
        response.setFunctionId(function.getFunctionId());
        response.setFunctionName(function.getFunctionName());
        response.setFunctionType(function.getFunctionType());
        response.setOwnerId(function.getOwner().getUserId());
        response.setIsPublic(function.getIsPublic());
        response.setPointsCount(function.getComputedPoints().size());
        response.setCreatedAt(function.getCreatedAt());
        return response;
    }
}