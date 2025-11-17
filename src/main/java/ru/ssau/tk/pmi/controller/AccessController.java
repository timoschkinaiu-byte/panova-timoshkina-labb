package ru.ssau.tk.pmi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.AccessDTO;
import ru.ssau.tk.pmi.entity.FunctionAccess;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.exceptions.FunctionNotFoundException;
import ru.ssau.tk.pmi.exceptions.UserNotFoundException;
import ru.ssau.tk.pmi.exceptions.AccessDeniedException;
import ru.ssau.tk.pmi.repository.FunctionAccessRepository;
import ru.ssau.tk.pmi.repository.MathFunctionRepository;
import ru.ssau.tk.pmi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    private static final Logger logger = LoggerFactory.getLogger(AccessController.class);
    private final FunctionAccessRepository accessRepository;
    private final MathFunctionRepository functionRepository;
    private final UserRepository userRepository;

    public AccessController(FunctionAccessRepository accessRepository,
                            MathFunctionRepository functionRepository,
                            UserRepository userRepository) {
        this.accessRepository = accessRepository;
        this.functionRepository = functionRepository;
        this.userRepository = userRepository;
    }



    @GetMapping
    public ResponseEntity<List<AccessDTO.Response>> getFunctionAccess(@RequestParam Long functionId) {
        logger.info("Получение доступа к функции: {}", functionId);

        try {
            // Получаем функцию, чтобы проверить существование
            MathFunction function = functionRepository.findById(functionId)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));


            List<FunctionAccess> accesses = accessRepository.findByFunction(function);
            List<AccessDTO.Response> response = accesses.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            logger.info("Найдено {} записей доступа для функции {}", response.size(), functionId);
            return ResponseEntity.ok(response);

        } catch (FunctionNotFoundException e) {
            logger.warn("Функция не найдена: {}", functionId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка получения доступа: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping
    public ResponseEntity<AccessDTO.Response> grantAccess(@RequestBody AccessDTO.GrantRequest request) {
        logger.info("Предоставление доступа: функция={}, пользователь={}, тип={}",
                request.getFunctionId(), request.getUserId(), request.getAccessType());

        try {
            validateGrantRequest(request);

            MathFunction function = functionRepository.findById(request.getFunctionId())
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

            // Проверяем, не существует ли уже такой доступ
            if (accessRepository.existsByFunctionAndUser(function, user)) {
                logger.warn("Доступ уже предоставлен: функция={}, пользователь={}",
                        request.getFunctionId(), request.getUserId());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            FunctionAccess access = new FunctionAccess();
            access.setAccessType(request.getAccessType());
            access.setFunction(function);
            access.setUser(user);

            FunctionAccess savedAccess = accessRepository.save(access);
            AccessDTO.Response response = convertToResponse(savedAccess);

            logger.info("Доступ предоставлен успешно. ID доступа: {}", savedAccess.getAccessId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (FunctionNotFoundException | UserNotFoundException e) {
            logger.warn("Ресурс не найден: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка предоставления доступа: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> revokeAccess(@RequestParam Long functionId, @RequestParam Long userId) {
        logger.info("Отзыв доступа: функция={}, пользователь={}", functionId, userId);

        try {
            validateRevokeRequest(functionId, userId);

            MathFunction function = functionRepository.findById(functionId)
                    .orElseThrow(() -> new FunctionNotFoundException("Функция не найдена"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

            // Проверяем существование доступа
            if (!accessRepository.existsByFunctionAndUser(function, user)) {
                logger.warn("Доступ не найден для отзыва: функция={}, пользователь={}", functionId, userId);
                return ResponseEntity.notFound().build();
            }

            accessRepository.deleteByFunctionAndUser(function, user);

            logger.info("Доступ успешно отозван");
            return ResponseEntity.noContent().build();

        } catch (FunctionNotFoundException | UserNotFoundException e) {
            logger.warn("Ресурс не найден: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка отзыва доступа: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Валидация
    private void validateGrantRequest(AccessDTO.GrantRequest request) {
        if (request.getFunctionId() == null) {
            throw new IllegalArgumentException("ID функции не может быть пустым");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("ID пользователя не может быть пустым");
        }
        if (request.getAccessType() == null ||
                (!"READ".equals(request.getAccessType()) && !"WRITE".equals(request.getAccessType()))) {
            throw new IllegalArgumentException("Тип доступа должен быть READ или WRITE");
        }
    }

    private void validateRevokeRequest(Long functionId, Long userId) {
        if (functionId == null) {
            throw new IllegalArgumentException("ID функции не может быть пустым");
        }
        if (userId == null) {
            throw new IllegalArgumentException("ID пользователя не может быть пустым");
        }
    }

    private void validateFunctionExists(Long functionId) {
        if (!functionRepository.existsById(functionId)) {
            throw new FunctionNotFoundException("Функция не найдена");
        }
    }

    // Конвертер
    private AccessDTO.Response convertToResponse(FunctionAccess access) {
        AccessDTO.Response response = new AccessDTO.Response();
        response.setAccessId(access.getAccessId());
        response.setUserId(access.getUser().getUserId());
        response.setUsername(access.getUser().getUsername());
        response.setAccessType(access.getAccessType());
        response.setGrantedAt(LocalDateTime.now());
        return response;
    }
}