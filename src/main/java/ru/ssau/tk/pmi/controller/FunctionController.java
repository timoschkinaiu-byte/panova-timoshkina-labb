package ru.ssau.tk.pmi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.FunctionDTO;
import ru.ssau.tk.pmi.dto.PointDTO;
import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.exceptions.AccessDeniedException;
import ru.ssau.tk.pmi.exceptions.FunctionNotFoundException;
import ru.ssau.tk.pmi.exceptions.InvalidFunctionException;
import ru.ssau.tk.pmi.functions.*;
import ru.ssau.tk.pmi.functions.factory.TabulatedFunctionFactory;
import ru.ssau.tk.pmi.functions.factory.ArrayTabulatedFunctionFactory;
import ru.ssau.tk.pmi.functions.factory.LinkedListTabulatedFunctionFactory;
import ru.ssau.tk.pmi.repository.MathFunctionRepository;
import ru.ssau.tk.pmi.repository.UserRepository;
import ru.ssau.tk.pmi.service.SecurityService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/functions")
public class FunctionController {

    private static final Logger logger = LoggerFactory.getLogger(FunctionController.class);
    private final MathFunctionRepository functionRepository;
    private final UserRepository userRepository;
    private final Map<String, ru.ssau.tk.pmi.functions.MathFunction> mathFunctionsMap;
    private TabulatedFunctionFactory currentFactory = new ArrayTabulatedFunctionFactory();
    private final SecurityService securityService;


    public FunctionController(MathFunctionRepository functionRepository, UserRepository userRepository, SecurityService securityService) {
        this.functionRepository = functionRepository;
        this.userRepository = userRepository;
        this.mathFunctionsMap = createMathFunctionsMap();
        this.securityService = securityService;
    }


    @PostMapping("/from-math-function")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> createFromMathFunction(
            @RequestParam(defaultValue = "ARRAY") String factoryType,
            @RequestBody FunctionDTO.CreateFromMathFunctionRequest request) {
        logger.info("Создание функции из MathFunction: {}, интервал [{}, {}], точек: {}",
                request.getSourceFunctionName(), request.getLeftX(), request.getRightX(), request.getPointsCount());

        try {
            validateCreateFromMathFunctionRequest(request);
            setFactoryByType(factoryType);

            ru.ssau.tk.pmi.functions.MathFunction sourceFunction = mathFunctionsMap.get(request.getSourceFunctionName());
            if (sourceFunction == null) {
                throw new InvalidFunctionException("Функция не найдена: " + request.getSourceFunctionName());
            }

            TabulatedFunction tabulatedFunction = createFromMathFunction(sourceFunction, request);
            MathFunction savedFunction = saveTabulatedFunction(tabulatedFunction, request.getName());

            FunctionDTO.Response response = convertToResponse(savedFunction);
            logger.info("Функция создана из MathFunction. ID: {}", savedFunction.getFunctionId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InvalidFunctionException e) {
            logger.warn("Функция не найдена: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные для создания из MathFunction: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка создания функции из MathFunction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PostMapping("/from-arrays")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> createFromArrays(
            @RequestParam(defaultValue = "ARRAY") String factoryType,
            @RequestBody FunctionDTO.CreateFromArraysRequest request){

        logger.info("RequestBody name={}, x={}, y={}",
                request.getName(),
                request.getXValues(),
                request.getYValues()
        );


        logger.info("Создание функции из массивов: {}, точек: {}", request.getName(),
                request.getXValues() != null ? request.getXValues().size() : 0);

        logger.info("Request class: {}", request.getClass());
        logger.info("Fields: name={}, xValues={}, yValues={}",
                request.getName(), request.getXValues(), request.getYValues());

        try {
            validateCreateFromArraysRequest(request);
            setFactoryByType(factoryType);

            double[] xValues = request.getXValues().stream().mapToDouble(Double::doubleValue).toArray();
            double[] yValues = request.getYValues().stream().mapToDouble(Double::doubleValue).toArray();



            logger.info("Создание функции из массивов: {}", request.getName());
            logger.info("xValues: {} (type: {})", request.getXValues(),
                    request.getXValues() != null ? request.getXValues().getClass() : "null");
            logger.info("yValues: {} (type: {})", request.getYValues(),
                    request.getYValues() != null ? request.getYValues().getClass() : "null");


            TabulatedFunction tabulatedFunction = currentFactory.create(xValues, yValues);
            MathFunction savedFunction = saveTabulatedFunction(tabulatedFunction, request.getName());

            FunctionDTO.Response response = convertToResponse(savedFunction);
            logger.info("Функция создана из массивов. ID: {}", savedFunction.getFunctionId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные для создания из массивов: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка создания функции из массивов: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }




    @PostMapping("/composite")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> createComposite(
            @RequestParam(defaultValue = "ARRAY") String factoryType,
            @RequestBody FunctionDTO.CreateCompositeRequest request) {
        logger.info("Создание сложной функции: {} ∘ {}", request.getOuterFunctionName(), request.getInnerFunctionName());

        try {
            validateCreateCompositeRequest(request);
            setFactoryByType(factoryType);

            ru.ssau.tk.pmi.functions.MathFunction outerFunction = mathFunctionsMap.get(request.getOuterFunctionName());
            ru.ssau.tk.pmi.functions.MathFunction innerFunction = mathFunctionsMap.get(request.getInnerFunctionName());

            if (outerFunction == null || innerFunction == null) {
                throw new InvalidFunctionException("Функции не найдены");
            }

            CompositeFunction compositeFunction = new CompositeFunction(outerFunction, innerFunction);

            // Табулируем сложную функцию
            TabulatedFunction tabulatedFunction = createFromMathFunction(compositeFunction,
                    new FunctionDTO.CreateFromMathFunctionRequest() {{
                        setName(request.getName());
                        setLeftX(-10.0);
                        setRightX(10.0);
                        setPointsCount(100);
                    }});

            MathFunction savedFunction = saveTabulatedFunction(tabulatedFunction, request.getName());

            FunctionDTO.Response response = convertToResponse(savedFunction);
            logger.info("Сложная функция создана. ID: {}", savedFunction.getFunctionId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InvalidFunctionException e) {
            logger.warn("Функции не найдены для создания сложной функции");
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка создания сложной функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FunctionDTO.Response>> getFunctions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Boolean isPublic) {

        logger.info("Поиск функций пользователем: {}", securityService.getCurrentUser().getUsername());

        try {
            List<MathFunction> functions;
            User currentUser = securityService.getCurrentUser();
            boolean isAdmin = "ADMIN".equals(currentUser.getRole());

            if (isAdmin) {
                // Админ видит все функции
                if (search != null && type != null) {
                    functions = functionRepository.findByNameAndType(search, type);
                } else if (search != null) {
                    functions = functionRepository.findByFunctionNameContainingIgnoreCase(search);
                } else if (type != null) {
                    functions = functionRepository.findByFunctionType(type);
                } else if (ownerId != null) {
                    User owner = userRepository.findById(ownerId).orElse(null);
                    functions = owner != null ? functionRepository.findByOwner(owner) : List.of();
                } else if (isPublic != null) {
                    functions = isPublic ? functionRepository.findByIsPublicTrue() : functionRepository.findAll();
                } else {
                    functions = functionRepository.findAll();
                }
            } else {
                // Пользователь видит только свои и публичные функции
                Long currentUserId = currentUser.getUserId();

                if (search != null && type != null) {
                    functions = functionRepository.findByNameAndTypeForUser(search, type, currentUserId);
                } else if (search != null) {
                    functions = functionRepository.findByFunctionNameContainingIgnoreCaseAndAccessible(search, currentUserId);
                } else if (type != null) {
                    functions = functionRepository.findByFunctionTypeAndAccessible(type, currentUserId);
                } else if (ownerId != null && ownerId.equals(currentUserId)) {
                    functions = functionRepository.findByOwner(currentUser);
                } else if (isPublic != null) {
                    if (isPublic) {
                        functions = functionRepository.findByIsPublicTrue();
                    } else {
                        functions = functionRepository.findByOwner(currentUser);
                    }
                } else {
                    functions = functionRepository.findAccessibleFunctions(currentUserId);
                }
            }

            List<FunctionDTO.Response> response = functions.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            logger.info("Найдено {} функций для пользователя {}", response.size(), currentUser.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Ошибка поиска функций: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{id}/points")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<PointDTO.Response>> getFunctionPoints(@PathVariable Long id) {
        logger.info("Получение точек функции: {}", id);

        try {
            // ПРОВЕРКА ПРАВ ПРОСМОТРА
            if (!securityService.canViewFunction(id)) {
                logger.warn("Отказано в доступе к точкам функции: {}", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            MathFunction function = functionRepository.findById(id)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            List<PointDTO.Response> points = function.getComputedPoints().stream()
                    .map(point -> {
                        PointDTO.Response response = new PointDTO.Response();
                        response.setPointId(point.getPointId());
                        response.setFunctionId(id);
                        response.setXValue(point.getXValue());
                        response.setYValue(point.getYValue());
                        return response;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(points);

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            logger.warn("Доступ запрещен к точкам функции: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Ошибка получения точек функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> getFunctionById(@PathVariable Long id) {
        logger.info("Получение функции по ID: {}", id);

        try {
            //проверка прав доступа
            if (!securityService.canViewFunction(id)) {
                logger.warn("Отказано в доступе к функции: {}", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            MathFunction function = functionRepository.findById(id)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            return ResponseEntity.ok(convertToResponse(function));

        } catch (FunctionNotFoundException e) {
            logger.warn("Функция не найдена: {}", id);
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            logger.warn("Доступ запрещен к функции: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Ошибка получения функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.Response> updateFunction(
            @PathVariable Long id,
            @RequestBody FunctionDTO.UpdateRequest request) {
        logger.info("Обновление функции: {}", id);

        try {
            // ПРОВЕРКА ПРАВ РЕДАКТИРОВАНИЯ
            securityService.checkFunctionOwnership(id);

            MathFunction function = functionRepository.findById(id)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));


            if (request.getFunctionName() != null) {
                function.setFunctionName(request.getFunctionName());
            }
            if (request.getIsPublic() != null) {
                function.setIsPublic(request.getIsPublic());
            }

            function.setUpdatedAt(LocalDateTime.now());
            MathFunction updatedFunction = functionRepository.save(function);

            logger.info("Функция обновлена: {}", id);
            return ResponseEntity.ok(convertToResponse(updatedFunction));

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            logger.warn("Отказано в обновлении функции: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Ошибка обновления функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFunction(@PathVariable Long id) {
        logger.info("Удаление функции: {}", id);

        try {
            securityService.checkFunctionOwnership(id);

            if (!functionRepository.existsById(id)) {
                throw new FunctionNotFoundException("Функция не найдена");
            }

            functionRepository.deleteById(id);
            logger.info("Функция удалена: {}", id);
            return ResponseEntity.noContent().build();

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            logger.warn("Отказано в удалении функции: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Ошибка удаления функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PostMapping("/{id}/compute")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.ComputeResponse> computeFunctionValue(
            @PathVariable Long id,
            @RequestBody FunctionDTO.ComputeRequest request) {
        logger.info("Вычисление значения функции {} в точке: {}", id, request.getX());

        try {
            if (!securityService.canViewFunction(id)) {
                logger.warn("Отказано в вычислении значения функции: {}", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            ru.ssau.tk.pmi.entity.MathFunction function = functionRepository.findById(id)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            // Получаем TabulatedFunction из entity
            TabulatedFunction tabulatedFunction = getTabulatedFunctionFromEntity(function);

            // Вычисляем значение
            double y = tabulatedFunction.apply(request.getX());

            // Определяем, было ли интерполирование
            boolean interpolated = (request.getX() < tabulatedFunction.leftBound() ||
                    request.getX() > tabulatedFunction.rightBound());

            FunctionDTO.ComputeResponse response = new FunctionDTO.ComputeResponse();
            response.setX(request.getX());
            response.setY(y);
            response.setInterpolated(interpolated);

            logger.info("Вычислено значение: f({}) = {}, интерполировано: {}", request.getX(), y, interpolated);
            return ResponseEntity.ok(response);

        } catch (FunctionNotFoundException e) {
            logger.warn("Функция не найдена: {}", id);
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            logger.warn("Доступ запрещен: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Ошибка вычисления значения функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private TabulatedFunction getTabulatedFunctionFromEntity(ru.ssau.tk.pmi.entity.MathFunction function) {
        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        function.getComputedPoints().forEach(point -> {
            xValues.add(point.getXValue());
            yValues.add(point.getYValue());
        });

        // Сортируем точки по X для корректной работы TabulatedFunction
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < xValues.size(); i++) indices.add(i);
        indices.sort(Comparator.comparing(xValues::get));

        double[] sortedX = new double[xValues.size()];
        double[] sortedY = new double[yValues.size()];

        for (int i = 0; i < indices.size(); i++) {
            int idx = indices.get(i);
            sortedX[i] = xValues.get(idx);
            sortedY[i] = yValues.get(idx);
        }

        return new ArrayTabulatedFunction(sortedX, sortedY);
    }



    @GetMapping("/{id}/graph-data")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FunctionDTO.GraphDataResponse> getGraphData(
            @PathVariable Long id,
            @RequestParam(defaultValue = "200") Integer pointsCount,
            @RequestParam(required = false) Double xFrom,
            @RequestParam(required = false) Double xTo) {
        logger.info("Получение данных графика функции: {}, точек: {}", id, pointsCount);

        try {
            //  ПРОВЕРКА ПРАВ ПРОСМОТРА
            if (!securityService.canViewFunction(id)) {
                logger.warn("Отказано в доступе к графику функции: {}", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            MathFunction function = functionRepository.findById(id)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            TabulatedFunction tabulatedFunction = getTabulatedFunctionFromMathFunction(function);

            // Определяем диапазон
            double leftX = xFrom != null ? xFrom : tabulatedFunction.leftBound();
            double rightX = xTo != null ? xTo : tabulatedFunction.rightBound();

            // Создаем данные для графика
            FunctionDTO.GraphDataResponse response = createGraphData(tabulatedFunction, leftX, rightX, pointsCount);
            response.setFunctionId(id);
            response.setFunctionName(function.getFunctionName());

            return ResponseEntity.ok(response);

        } catch (FunctionNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            logger.warn("Доступ к графику функции запрещен: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Ошибка получения данных графика: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Вспомогательные методы
    private void setFactoryByType(String factoryType) {
        switch (factoryType) {
            case "LINKED_LIST":
                currentFactory = new LinkedListTabulatedFunctionFactory();
                break;
            case "ARRAY":
            default:
                currentFactory = new ArrayTabulatedFunctionFactory();
        }
    }


    private TabulatedFunction createFromMathFunction(ru.ssau.tk.pmi.functions.MathFunction sourceFunction,
                                                     FunctionDTO.CreateFromMathFunctionRequest request) {
        // Создаем массивы для табулированной функции
        int pointsCount = request.getPointsCount();
        double[] xValues = new double[pointsCount];
        double[] yValues = new double[pointsCount];

        double step = (request.getRightX() - request.getLeftX()) / (pointsCount - 1);

        for (int i = 0; i < pointsCount; i++) {
            xValues[i] = request.getLeftX() + i * step;
            yValues[i] = sourceFunction.apply(xValues[i]);
        }

        // Используем фабрику для создания табулированной функции
        return currentFactory.create(xValues, yValues);
    }



    private MathFunction saveTabulatedFunction(TabulatedFunction tabulatedFunction, String name) {
        User currentUser = securityService.getCurrentUser();

        MathFunction mathFunction = new MathFunction();
        mathFunction.setFunctionName(name);
        mathFunction.setFunctionDefinition("Табулированная функция");
        mathFunction.setFunctionType("TABULATED");
        mathFunction.setIsPublic(false);
        mathFunction.setCreatedAt(LocalDateTime.now());
        mathFunction.setUpdatedAt(LocalDateTime.now());
        mathFunction.setOwner(currentUser);

        // СОХРАНЯЕМ ТОЧКИ
        List<ComputedPoint> points = new ArrayList<>();
        for (int i = 0; i < tabulatedFunction.getCount(); i++) {
            ComputedPoint point = new ComputedPoint();
            point.setXValue(tabulatedFunction.getX(i));
            point.setYValue(tabulatedFunction.getY(i));
            point.setFunction(mathFunction);
            points.add(point);
        }
        mathFunction.setComputedPoints(points);

        return functionRepository.save(mathFunction);
    }

    private Map<String, ru.ssau.tk.pmi.functions.MathFunction> createMathFunctionsMap() {
        Map<String, ru.ssau.tk.pmi.functions.MathFunction> map = new LinkedHashMap<>();
        map.put("Квадратичная функция", new SqrFunction());
        map.put("Тождественная функция", new IdentityFunction());
        map.put("Постоянная функция", new ConstantFunction(1.0));
        map.put("Единичная функция", new UnitFunction());
        map.put("Нулевая функция", new ZeroFunction());
        return map;
    }


    // Валидация
    private void validateCreateFromArraysRequest(FunctionDTO.CreateFromArraysRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя функции не может быть пустым");
        }
        if (request.getXValues() == null || request.getYValues() == null) {
            throw new IllegalArgumentException("Массивы X и Y не могут быть пустыми");
        }
        if (request.getXValues().size() != request.getYValues().size()) {
            throw new IllegalArgumentException("Массивы X и Y должны быть одинаковой длины");
        }
        if (request.getXValues().size() < 2) {
            throw new IllegalArgumentException("Функция должна содержать минимум 2 точки");
        }
    }

    private void validateCreateFromMathFunctionRequest(FunctionDTO.CreateFromMathFunctionRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя функции не может быть пустым");
        }
        if (request.getSourceFunctionName() == null) {
            throw new IllegalArgumentException("Исходная функция не может быть пустой");
        }
        if (request.getPointsCount() == null || request.getPointsCount() < 2) {
            throw new IllegalArgumentException("Количество точек должно быть не менее 2");
        }
        if (request.getLeftX() >= request.getRightX()) {
            throw new IllegalArgumentException("Левый край должен быть меньше правого");
        }
    }

    private void validateCreateCompositeRequest(FunctionDTO.CreateCompositeRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя функции не может быть пустым");
        }
        if (request.getOuterFunctionName() == null || request.getInnerFunctionName() == null) {
            throw new IllegalArgumentException("Внешняя и внутренняя функции не могут быть пустыми");
        }
    }


    private TabulatedFunction getTabulatedFunctionFromMathFunction(MathFunction function) {
        // Создаем TabulatedFunction из точек MathFunction
        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        function.getComputedPoints().forEach(point -> {
            xValues.add(point.getXValue());
            yValues.add(point.getYValue());
        });

        return new ArrayTabulatedFunction(
                xValues.stream().mapToDouble(Double::doubleValue).toArray(),
                yValues.stream().mapToDouble(Double::doubleValue).toArray()
        );
    }

    private FunctionDTO.GraphDataResponse createGraphData(TabulatedFunction function, double leftX, double rightX, int pointsCount) {
        FunctionDTO.GraphDataResponse response = new FunctionDTO.GraphDataResponse();
        List<FunctionDTO.GraphPoint> points = new ArrayList<>();

        double step = (rightX - leftX) / (pointsCount - 1);
        for (int i = 0; i < pointsCount; i++) {
            double x = leftX + i * step;
            double y = function.apply(x);

            FunctionDTO.GraphPoint point = new FunctionDTO.GraphPoint();
            point.setX(x);
            point.setY(y);
            points.add(point);
        }

        response.setPoints(points);

        // Вычисляем диапазоны
        FunctionDTO.ValueRange xRange = new FunctionDTO.ValueRange();
        xRange.setMin(leftX);
        xRange.setMax(rightX);
        response.setXRange(xRange);

        // Вычисляем Y диапазон
        double minY = points.stream().mapToDouble(FunctionDTO.GraphPoint::getY).min().orElse(0);
        double maxY = points.stream().mapToDouble(FunctionDTO.GraphPoint::getY).max().orElse(0);
        FunctionDTO.ValueRange yRange = new FunctionDTO.ValueRange();
        yRange.setMin(minY);
        yRange.setMax(maxY);
        response.setYRange(yRange);

        return response;
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
