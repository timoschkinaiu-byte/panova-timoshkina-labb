package ru.ssau.tk.pmi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.ssau.tk.pmi.dto.UserDTO;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.exceptions.UserAlreadyExistsException;
import ru.ssau.tk.pmi.exceptions.UserNotFoundException;
import ru.ssau.tk.pmi.exceptions.InvalidCredentialsException;
import ru.ssau.tk.pmi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO.Response> register(@RequestBody UserDTO.RegisterRequest request) {
        logger.info("Попытка регистрации пользователя: {}", request.getUsername());

        try {
            validateRegisterRequest(request);

            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new UserAlreadyExistsException("Пользователь с именем " + request.getUsername() + " уже существует");
            }

            //  PasswordEncoder для хеширования пароля
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRole("USER"); // По умолчанию роль USER
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(user);
            UserDTO.Response response = convertToResponse(savedUser);

            logger.info("Пользователь успешно зарегистрирован: {}", savedUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserAlreadyExistsException e) {
            logger.warn("Попытка регистрации существующего пользователя: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные при регистрации: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка регистрации: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @GetMapping("/me")
    public ResponseEntity<UserDTO.Response> getCurrentUser() {
        logger.info("Запрос информации о текущем пользователе");

        try {
            // получаем текущего аутентифицированного пользователя
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();

            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new UserNotFoundException("Текущий пользователь не найден"));

            UserDTO.Response response = convertToResponse(currentUser);
            logger.info("Текущий пользователь: {}", currentUsername);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException e) {
            logger.warn("Текущий пользователь не найден в БД");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error("Ошибка получения текущего пользователя: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#id)")
    public ResponseEntity<UserDTO.Response> getUserById(@PathVariable Long id) {
        logger.info("Запрос пользователя по ID: {}", id);

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

            UserDTO.Response response = convertToResponse(user);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException e) {
            logger.warn("Пользователь с ID {} не найден", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка получения пользователя: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.username")
    public ResponseEntity<UserDTO.Response> updateUser(@PathVariable Long id, @RequestBody UserDTO.UpdateRequest request) {
        logger.info("Обновление пользователя с ID: {}", id);

        try {
            validateUpdateRequest(request);

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

            // Проверяем, не занято ли новое имя пользователя
            if (request.getUsername() != null && !user.getUsername().equals(request.getUsername())) {
                if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                    throw new UserAlreadyExistsException("Имя пользователя уже занято(");
                }
                user.setUsername(request.getUsername());
            }

            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);

            UserDTO.Response response = convertToResponse(updatedUser);
            logger.info("Пользователь с ID {} успешно обновлен", id);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException e) {
            logger.warn("Пользователь с ID {} не найден для обновления", id);
            return ResponseEntity.notFound().build();
        } catch (UserAlreadyExistsException e) {
            logger.warn("Имя пользователя уже занято: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверные данные при обновлении пользователя: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка обновления пользователя: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO.Response> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        logger.info("Изменение роли пользователя с ID {} на: {}", id, role);

        try {
            validateRole(role);

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

            user.setRole(role);
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);

            UserDTO.Response response = convertToResponse(updatedUser);
            logger.info("Роль пользователя с ID {} успешно изменена на {}", id, role);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException e) {
            logger.warn("Пользователь с ID {} не найден для изменения роли", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Неверная роль: {}", role);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Ошибка изменения роли пользователя: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("Удаление пользователя с ID: {}", id);

        try {
            if (!userRepository.existsById(id)) {
                throw new UserNotFoundException("Пользователь не найден");
            }

            userRepository.deleteById(id);
            logger.info("Пользователь с ID {} успешно удален", id);
            return ResponseEntity.noContent().build();

        } catch (UserNotFoundException e) {
            logger.warn("Пользователь с ID {} не найден для удаления", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Ошибка удаления пользователя: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO.ShortResponse>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        logger.info("Поиск пользователей: search='{}', role='{}'", search, role);

        try {
            List<User> users;

            if (search != null && role != null) {
                // Фильтруем вручную, так как метода findByUsernameContainingIgnoreCaseAndRole нет
                users = userRepository.findByUsernameContainingIgnoreCase(search).stream()
                        .filter(user -> role.equals(user.getRole()))
                        .collect(Collectors.toList());
            } else if (search != null) {
                users = userRepository.findByUsernameContainingIgnoreCase(search);
            } else if (role != null) {
                users = userRepository.findByRole(role);
            } else {
                users = userRepository.findAll();
            }

            List<UserDTO.ShortResponse> response = users.stream()
                    .map(this::convertToShortResponse)
                    .collect(Collectors.toList());

            logger.info("Найдено {} пользователей", response.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Ошибка поиска пользователей: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    // Валидация методов
    private void validateRegisterRequest(UserDTO.RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов");
        }
    }


    private void validateUpdateRequest(UserDTO.UpdateRequest request) {
        if (request.getUsername() != null && request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
    }

    private void validateRole(String role) {
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Недопустимая роль: " + role);
        }
    }

    // Конвертеры
    private UserDTO.Response convertToResponse(User user) {
        UserDTO.Response response = new UserDTO.Response();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private UserDTO.ShortResponse convertToShortResponse(User user) {
        UserDTO.ShortResponse response = new UserDTO.ShortResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        return response;
    }
}