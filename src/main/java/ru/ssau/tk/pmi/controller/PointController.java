package ru.ssau.tk.pmi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.PointDTO;
import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.exceptions.FunctionNotFoundException;
import ru.ssau.tk.pmi.exceptions.PointNotFoundException;
import ru.ssau.tk.pmi.functions.Insertable;
import ru.ssau.tk.pmi.functions.Removable;
import ru.ssau.tk.pmi.functions.TabulatedFunction;
import ru.ssau.tk.pmi.functions.ArrayTabulatedFunction;
import ru.ssau.tk.pmi.repository.ComputedPointRepository;
import ru.ssau.tk.pmi.repository.MathFunctionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/points")
public class PointController {

    private static final Logger logger = LoggerFactory.getLogger(PointController.class);
    private final ComputedPointRepository pointRepository;
    private final MathFunctionRepository functionRepository;

    public PointController(ComputedPointRepository pointRepository, MathFunctionRepository functionRepository) {
        this.pointRepository = pointRepository;
        this.functionRepository = functionRepository;
    }

    @GetMapping
    public ResponseEntity<List<PointDTO.Response>> getPoints(
            @RequestParam Long functionId,
            @RequestParam(required = false) Double xFrom,
            @RequestParam(required = false) Double xTo) {
        logger.info("Получение точек функции {} в диапазоне [{}, {}]", functionId, xFrom, xTo);

        try {
            MathFunction function = functionRepository.findById(functionId)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            List<ComputedPoint> points;
            if (xFrom != null && xTo != null) {
                points = pointRepository.findByFunctionAndXValueBetween(function, xFrom, xTo);
            } else {
                points = pointRepository.findByFunction(function);
                // Фильтруем вручную если нужен диапазон
                if (xFrom != null || xTo != null) {
                    points = points.stream()
                            .filter(point -> (xFrom == null || point.getXValue() >= xFrom) &&
                                    (xTo == null || point.getXValue() <= xTo))
                            .collect(Collectors.toList());
                }
            }

            List<PointDTO.Response> response = points.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            logger.info("Найдено {} точек", response.size());
            return ResponseEntity.ok(response);

        } catch (FunctionNotFoundException e) {
            logger.warn("Функция не найдена: {}", functionId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка получения точек: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<PointDTO.Response> createPoint(@RequestBody PointDTO.CreateRequest request) {
        logger.info("Добавление точки: функция={}, x={}, y={}",
                request.getFunctionId(), request.getXValue(), request.getYValue());

        try {
            validateCreateRequest(request);

            MathFunction function = functionRepository.findById(request.getFunctionId())
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            // Проверяем, существует ли уже точка с таким X (фильтруем вручную)
            boolean pointExists = pointRepository.findByFunction(function).stream()
                    .anyMatch(point -> point.getXValue().equals(request.getXValue()));

            if (pointExists) {
                logger.warn("Точка с x={} уже существует для функции {}", request.getXValue(), request.getFunctionId());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            ComputedPoint point = new ComputedPoint();
            point.setXValue(request.getXValue());
            point.setYValue(request.getYValue());
            point.setFunction(function);

            ComputedPoint savedPoint = pointRepository.save(point);
            PointDTO.Response response = convertToResponse(savedPoint);

            logger.info("Точка добавлена. ID: {}", savedPoint.getPointId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (FunctionNotFoundException e) {
            logger.warn("Функция не найдена: {}", request.getFunctionId());
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные точки: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка добавления точки: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PointDTO.Response> getPointById(@PathVariable Long id) {
        logger.info("Получение точки по ID: {}", id);

        try {
            ComputedPoint point = pointRepository.findById(id)
                    .orElseThrow(() -> new PointNotFoundException("Точка не найдена"));

            return ResponseEntity.ok(convertToResponse(point));

        } catch (PointNotFoundException e) {
            logger.warn("Точка не найдена: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка получения точки: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PointDTO.Response> updatePoint(
            @PathVariable Long id,
            @RequestBody PointDTO.UpdateRequest request) {
        logger.info("Обновление точки {}: y={}", id, request.getYValue());

        try {
            validateUpdateRequest(request);

            ComputedPoint point = pointRepository.findById(id)
                    .orElseThrow(() -> new PointNotFoundException("Точка не найдена"));

            point.setYValue(request.getYValue());
            ComputedPoint updatedPoint = pointRepository.save(point);

            logger.info("Точка {} обновлена", id);
            return ResponseEntity.ok(convertToResponse(updatedPoint));

        } catch (PointNotFoundException e) {
            logger.warn("Точка не найдена: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные для обновления: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка обновления точки: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePoint(@PathVariable Long id) {
        logger.info("Удаление точки: {}", id);

        try {
            if (!pointRepository.existsById(id)) {
                throw new PointNotFoundException("Точка не найдена");
            }

            pointRepository.deleteById(id);
            logger.info("Точка {} удалена", id);
            return ResponseEntity.noContent().build();

        } catch (PointNotFoundException e) {
            logger.warn("Точка не найдена: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка удаления точки: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PointDTO.Response>> searchPoints(
            @RequestParam(required = false) Double x,
            @RequestParam(required = false) Double y,
            @RequestParam(required = false) Long functionId) {
        logger.info("Поиск точек: x={}, y={}, functionId={}", x, y, functionId);

        try {
            List<ComputedPoint> points;

            if (functionId != null) {
                MathFunction function = functionRepository.findById(functionId)
                        .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

                if (x != null && y != null) {
                    points = pointRepository.findByExactValues(x, y).stream()
                            .filter(point -> point.getFunction().equals(function))
                            .collect(Collectors.toList());
                } else if (x != null) {
                    points = pointRepository.findByFunction(function).stream()
                            .filter(point -> point.getXValue().equals(x))
                            .collect(Collectors.toList());
                } else if (y != null) {
                    points = pointRepository.findByFunctionAndyValue(function, y);
                } else {
                    points = pointRepository.findByFunction(function);
                }
            } else {
                if (x != null && y != null) {
                    points = pointRepository.findByExactValues(x, y);
                } else if (x != null) {
                    points = pointRepository.findByxValueBetween(x, x); // Точечное значение
                } else if (y != null) {
                    points = pointRepository.findByyValueBetween(y, y); // Точечное значение
                } else {
                    points = pointRepository.findAll();
                }
            }

            List<PointDTO.Response> response = points.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            logger.info("Найдено {} точек", response.size());
            return ResponseEntity.ok(response);

        } catch (FunctionNotFoundException e) {
            logger.warn("Функция не найдена: {}", functionId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка поиска точек: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Конвертация MathFunction в TabulatedFunction
    private TabulatedFunction getTabulatedFunctionFromEntity(MathFunction function) {
        List<ComputedPoint> points = pointRepository.findByFunctionOrderByxValue(function);

        double[] xValues = new double[points.size()];
        double[] yValues = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            ComputedPoint point = points.get(i);
            xValues[i] = point.getXValue();
            yValues[i] = point.getYValue();
        }

        return new ArrayTabulatedFunction(xValues, yValues);
    }

    // Валидация
    private void validateCreateRequest(PointDTO.CreateRequest request) {
        if (request.getFunctionId() == null) {
            throw new IllegalArgumentException("ID функции не может быть пустым");
        }
        if (request.getXValue() == null) {
            throw new IllegalArgumentException("X значение не может быть пустым");
        }
        if (request.getYValue() == null) {
            throw new IllegalArgumentException("Y значение не может быть пустым");
        }
    }

    private void validateUpdateRequest(PointDTO.UpdateRequest request) {
        if (request.getYValue() == null) {
            throw new IllegalArgumentException("Y значение не может быть пустым");
        }
    }

    // Конвертер
    private PointDTO.Response convertToResponse(ComputedPoint point) {
        PointDTO.Response response = new PointDTO.Response();
        response.setPointId(point.getPointId());
        response.setFunctionId(point.getFunction().getFunctionId());
        response.setXValue(point.getXValue());
        response.setYValue(point.getYValue());
        return response;
    }
}