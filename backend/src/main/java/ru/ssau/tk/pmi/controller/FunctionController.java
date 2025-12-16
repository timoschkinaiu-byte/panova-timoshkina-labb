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


    @GetMapping("/available-for-composite")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FunctionDTO.AvailableForCompositeDTO>> getFunctionsForComposite() {
        logger.info("Получение функций доступных для создания composite");

        try {
            User currentUser = securityService.getCurrentUser();
            List<FunctionDTO.AvailableForCompositeDTO> result = new ArrayList<>();

            // 1. Базовые функции (SQR, IDENTITY, etc.)
            result.add(new FunctionDTO.AvailableForCompositeDTO("SQR", "Квадратичная функция", "BASIC"));
            result.add(new FunctionDTO.AvailableForCompositeDTO("IDENTITY", "Тождественная функция", "BASIC"));
            result.add(new FunctionDTO.AvailableForCompositeDTO("CONSTANT_1.0", "Постоянная функция (1.0)", "BASIC"));
            result.add(new FunctionDTO.AvailableForCompositeDTO("UNIT", "Единичная функция", "BASIC"));
            result.add(new FunctionDTO.AvailableForCompositeDTO("ZERO", "Нулевая функция", "BASIC"));

            // 2. Пользовательские функции текущего пользователя
            List<MathFunction> userFunctions = functionRepository.findByOwner(currentUser);
            for (MathFunction func : userFunctions) {
                result.add(new FunctionDTO.AvailableForCompositeDTO(
                        "USER_" + func.getFunctionId(),
                        func.getFunctionName() + " (ID: " + func.getFunctionId() + ")",
                        "USER"
                ));
            }

            // 3. Публичные функции других пользователей
            List<MathFunction> publicFunctions = functionRepository.findByIsPublicTrue();
            for (MathFunction func : publicFunctions) {
                if (!func.getOwner().getUserId().equals(currentUser.getUserId())) {
                    result.add(new FunctionDTO.AvailableForCompositeDTO(
                            "USER_" + func.getFunctionId(),
                            func.getFunctionName() + " (публичная, ID: " + func.getFunctionId() + ")",
                            "PUBLIC"
                    ));
                }
            }

            logger.info("Возвращено {} функций для composite", result.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Ошибка получения функций для composite: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

            ru.ssau.tk.pmi.functions.MathFunction sourceFunction;

            try {
                // Пробуем получить функцию через resolveMathFunction
                sourceFunction = resolveMathFunction(request.getSourceFunctionName());
            } catch (InvalidFunctionException e) {
                // Если не нашли, логируем и возвращаем ошибку
                logger.warn("Функция не найдена в resolveMathFunction: {}", request.getSourceFunctionName());
                throw e; // Пробрасываем дальше
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


        logger.info("Создание сложной функции: {} ∘ {}",
                request.getOuterFunctionName(), request.getInnerFunctionName());



        try {

            validateCreateCompositeRequest(request);
            logger.info("2");
            setFactoryByType(factoryType);
            logger.info("3");

            // 1. Получаем или загружаем функции
            ru.ssau.tk.pmi.functions.MathFunction outerFunction =
                    resolveMathFunction(request.getOuterFunctionName());
            ru.ssau.tk.pmi.functions.MathFunction innerFunction =
                    resolveMathFunction(request.getInnerFunctionName());
            logger.info("4");

            // 2. Создаем composite
            CompositeFunction compositeFunction = new CompositeFunction(outerFunction, innerFunction);

            // 3. Табулируем сложную функцию
            TabulatedFunction tabulatedFunction = createFromMathFunction(compositeFunction,
                    new FunctionDTO.CreateFromMathFunctionRequest() {{
                        setName(request.getName());
                        setLeftX(-10.0);
                        setRightX(10.0);
                        setPointsCount(100);
                    }});

            // 4. Сохраняем как обычную функцию
            MathFunction savedFunction = saveTabulatedFunction(tabulatedFunction, request.getName());

            // 5. Добавляем созданную composite функцию в мапу для будущего использования
            addFunctionToMathMap(savedFunction);

            FunctionDTO.Response response = convertToResponse(savedFunction);
            logger.info("Сложная функция создана. ID: {}", savedFunction.getFunctionId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (InvalidFunctionException e) {
            logger.warn("Функции не найдены для создания сложной функции: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка создания сложной функции: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ru.ssau.tk.pmi.functions.MathFunction resolveMathFunction(String functionKey) {
        logger.info("Поиск функции по ключу: {}", functionKey);

        // 1. Проверяем базовые функции
        switch (functionKey) {
            case "SQR":
                logger.info("Найдена базовая функция: SQR");
                return new SqrFunction();
            case "IDENTITY":
                logger.info("Найдена базовая функция: IDENTITY");
                return new IdentityFunction();
            case "CONSTANT_1.0":
                logger.info("Найдена базовая функция: CONSTANT_1.0");
                return new ConstantFunction(1.0);
            case "UNIT":
                logger.info("Найдена базовая функция: UNIT");
                return new UnitFunction();
            case "ZERO":
                logger.info("Найдена базовая функция: ZERO");
                return new ZeroFunction();
        }

        // 2. Проверяем, начинается ли с "USER_" (пользовательская функция)
        if (functionKey.startsWith("USER_")) {
            logger.info("Обработка пользовательской функции: {}", functionKey);
            try {
                Long functionId = Long.parseLong(functionKey.substring(5));
                logger.info("Ищем функцию в БД по ID: {}", functionId);

                MathFunction userFunc = functionRepository.findById(functionId)
                        .orElseThrow(() -> {
                            logger.error("Функция не найдена в БД по ID: {}", functionId);
                            return new InvalidFunctionException("Функция не найдена: " + functionKey);
                        });

                logger.info("Функция найдена в БД: ID={}, имя={}",
                        userFunc.getFunctionId(), userFunc.getFunctionName());

                // Проверяем доступ
                User currentUser = securityService.getCurrentUser();
                boolean canView = securityService.canViewFunction(functionId);

                if (!canView) {
                    logger.warn("Нет доступа к функции ID: {}", functionId);
                    throw new AccessDeniedException("Нет доступа к функции: " + functionKey);
                }

                // Создаем TabulatedFunction из точек БД
                logger.info("Конвертация функции ID {} в MathFunction", functionId);
                ru.ssau.tk.pmi.functions.MathFunction result = convertToMathFunction(userFunc);
                logger.info("Конвертация успешна для функции ID: {}", functionId);

                return result;

            } catch (NumberFormatException e) {
                logger.error("Некорректный ID функции: {}", functionKey);
                throw new InvalidFunctionException("Некорректный ID функции: " + functionKey);
            } catch (AccessDeniedException e) {
                logger.warn("Доступ запрещен к функции: {}", functionKey);
                throw e;
            } catch (Exception e) {
                logger.error("Ошибка при загрузке функции {}: {}", functionKey, e.getMessage(), e);
                throw new InvalidFunctionException("Ошибка загрузки функции: " + functionKey);
            }
        }

        // 3. Проверяем другие форматы
        if (functionKey.startsWith("CONSTANT_")) {
            try {
                double value = Double.parseDouble(functionKey.substring(9));
                logger.info("Создание ConstantFunction со значением: {}", value);
                return new ConstantFunction(value);
            } catch (NumberFormatException e) {
                throw new InvalidFunctionException("Некорректное значение константы: " + functionKey);
            }
        }

        logger.error("Функция не найдена ни в одном из форматов: {}", functionKey);
        throw new InvalidFunctionException("Функция не найдена: " + functionKey);
    }



    private ru.ssau.tk.pmi.functions.MathFunction convertToMathFunction(MathFunction entity) {
        logger.info("Начало конвертации функции ID {} в MathFunction", entity.getFunctionId());

        try {
            // Получаем точки функции
            List<ComputedPoint> points = entity.getComputedPoints();

            if (points == null) {
                logger.error("points == null для функции ID {}", entity.getFunctionId());
                throw new InvalidFunctionException("Функция не содержит точек (points is null)");
            }

            logger.info("У функции ID {} найдено {} точек", entity.getFunctionId(), points.size());

            if (points.isEmpty()) {
                logger.error("Функция ID {} не имеет точек (empty)", entity.getFunctionId());
                throw new InvalidFunctionException("Функция не содержит точек");
            }

            // Сортируем точки по X
            points.sort(Comparator.comparing(ComputedPoint::getXValue));

            // Создаем массивы
            double[] xValues = new double[points.size()];
            double[] yValues = new double[points.size()];

            for (int i = 0; i < points.size(); i++) {
                ComputedPoint point = points.get(i);
                xValues[i] = point.getXValue();
                yValues[i] = point.getYValue();
                logger.debug("Точка {}: x={}, y={}", i, xValues[i], yValues[i]);
            }

            logger.info("Создана TabulatedFunction из {} точек для функции ID {}",
                    points.size(), entity.getFunctionId());

            // Создаем TabulatedFunction
            return new ArrayTabulatedFunction(xValues, yValues);

        } catch (Exception e) {
            logger.error("Ошибка конвертации функции ID {}: {}",
                    entity.getFunctionId(), e.getMessage(), e);
            throw new InvalidFunctionException("Не удалось преобразовать функцию: " + e.getMessage());
        }
    }

    private void addFunctionToMathMap(MathFunction function) {
        // Добавляем пользовательскую функцию в мапу для будущего использования
        String key = "USER_" + function.getFunctionId();
        TabulatedFunction tabulatedFunction = getTabulatedFunctionFromEntity(function);
        mathFunctionsMap.put(key, tabulatedFunction);
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

        // 2. Загружаем пользовательские функции при старте
        loadUserFunctionsIntoMap(map);
        return map;
    }


    private void loadUserFunctionsIntoMap(Map<String, ru.ssau.tk.pmi.functions.MathFunction> map) {
        try {
            List<MathFunction> userFunctions = functionRepository.findAll();

            for (MathFunction userFunc : userFunctions) {
                // Создаем ключ для пользовательской функции
                String key = "USER_" + userFunc.getFunctionId();

                // Создаем TabulatedFunction из точек БД
                TabulatedFunction tabulatedFunction = getTabulatedFunctionFromEntity(userFunc);

                // Добавляем в мапу как MathFunction
                map.put(key, tabulatedFunction);

                // Также добавляем по имени (если уникально)
                String nameKey = "UFUNC_" + userFunc.getFunctionName().replaceAll("\\s+", "_");
                if (!map.containsKey(nameKey)) {
                    map.put(nameKey, tabulatedFunction);
                }
            }

            logger.info("Загружено {} пользовательских функций в mathFunctionsMap", userFunctions.size());
        } catch (Exception e) {
            logger.error("Ошибка загрузки пользовательских функций в мапу: {}", e.getMessage());
        }
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
        logger.info("запущена валидация");

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
        System.out.println("DEBUG: validateCreateCompositeRequest вызван");
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

    private ru.ssau.tk.pmi.functions.MathFunction loadUserFunction(String functionIdentifier) {
        // Пользовательские функции могут передаваться в формате:
        // 1. "USER_123" - по ID
        // 2. "123" - просто ID
        // 3. "МояФункция" - по имени (если уникально)

        try {
            // Вариант 1: "USER_123"
            if (functionIdentifier.startsWith("USER_")) {
                String idStr = functionIdentifier.substring(5);
                Long functionId = Long.parseLong(idStr);
                return getUserFunctionById(functionId);
            }

            // Вариант 2: Просто число "123"
            try {
                Long functionId = Long.parseLong(functionIdentifier);
                return getUserFunctionById(functionId);
            } catch (NumberFormatException e) {
                // Не число, пробуем по имени
            }

            // Вариант 3: По имени функции
            return getUserFunctionByName(functionIdentifier);

        } catch (Exception e) {
            logger.warn("Не удалось загрузить пользовательскую функцию: {}", functionIdentifier);
            return null;
        }
    }

    private ru.ssau.tk.pmi.functions.MathFunction getUserFunctionById(Long functionId) {
        MathFunction userFunc = functionRepository.findById(functionId)
                .orElseThrow(() -> new InvalidFunctionException("Функция не найдена по ID: " + functionId));

        // Проверяем доступ
        if (!securityService.canViewFunction(functionId)) {
            throw new AccessDeniedException("Нет доступа к функции: " + functionId);
        }

        return convertUserFunctionToMathFunction(userFunc);
    }

    private ru.ssau.tk.pmi.functions.MathFunction getUserFunctionByName(String functionName) {
        // Ищем функцию по имени среди доступных пользователю
        User currentUser = securityService.getCurrentUser();
        List<MathFunction> functions = functionRepository.findAccessibleFunctions(currentUser.getUserId());

        return functions.stream()
                .filter(f -> f.getFunctionName().equalsIgnoreCase(functionName))
                .findFirst()
                .map(this::convertUserFunctionToMathFunction)
                .orElseThrow(() -> new InvalidFunctionException("Функция не найдена по имени: " + functionName));
    }

    private ru.ssau.tk.pmi.functions.MathFunction convertUserFunctionToMathFunction(MathFunction entity) {
        // Конвертируем сохраненную функцию в TabulatedFunction
        List<ComputedPoint> points = entity.getComputedPoints();

        // Сортируем по X
        points.sort(Comparator.comparing(ComputedPoint::getXValue));

        double[] xValues = new double[points.size()];
        double[] yValues = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            xValues[i] = points.get(i).getXValue();
            yValues[i] = points.get(i).getYValue();
        }

        // Создаем TabulatedFunction
        return new ArrayTabulatedFunction(xValues, yValues);
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
