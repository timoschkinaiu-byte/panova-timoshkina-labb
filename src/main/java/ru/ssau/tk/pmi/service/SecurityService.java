package ru.ssau.tk.pmi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.exceptions.AccessDeniedException;
import ru.ssau.tk.pmi.repository.MathFunctionRepository;
import ru.ssau.tk.pmi.repository.UserRepository;

@Service
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    private final UserRepository userRepository;
    private final MathFunctionRepository functionRepository;

    public SecurityService(UserRepository userRepository, MathFunctionRepository functionRepository) {
        this.userRepository = userRepository;
        this.functionRepository = functionRepository;
    }


      //Получить текущего аутентифицированного пользователя

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Текущий пользователь не найден в БД: {}", username);
                    return new AccessDeniedException("Пользователь не найден");
                });
    }


     //Проверить, является ли пользователь владельцем функции

    public boolean isFunctionOwner(Long functionId, Long userId) {
        MathFunction function = functionRepository.findById(functionId)
                .orElseThrow(() -> {
                    logger.warn("Функция не найдена: {}", functionId);
                    return new AccessDeniedException("Функция не найдена");
                });

        boolean isOwner = function.getOwner().getUserId().equals(userId);
        logger.debug("Проверка владения: функция={}, пользователь={}, результат={}",
                functionId, userId, isOwner);
        return isOwner;
    }

    public void checkFunctionOwnership(Long functionId) {
        User currentUser = getCurrentUser();
        MathFunction function = functionRepository.findById(functionId)
                .orElseThrow(() -> {
                    logger.warn("Функция не найдена: {}", functionId);
                    return new AccessDeniedException("Функция не найдена");
                });

        boolean isOwner = function.getOwner().getUserId().equals(currentUser.getUserId());
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            logger.warn("Отказано в доступе: пользователь {} пытался получить доступ к функции {}",
                    currentUser.getUsername(), functionId);
            throw new AccessDeniedException("Нет прав доступа к этой функции");
        }

        logger.info("Доступ разрешен: пользователь {} -> функция {}",
                currentUser.getUsername(), functionId);
    }


      //Проверить, может ли пользователь редактировать функци (владелец или ADMIN)

    public boolean canEditFunction(Long functionId) {
        User currentUser = getCurrentUser();

        if ("ADMIN".equals(currentUser.getRole())) {
            return true;
        }

        return isFunctionOwner(functionId, currentUser.getUserId());
    }


    public boolean isCurrentUser(Long userId) {
        User currentUser = getCurrentUser();
        return currentUser.getUserId().equals(userId);
    }

    public void checkFunctionAccess(Long functionId) {
        if (!canViewFunction(functionId)) {
            User currentUser = getCurrentUser();
            logger.warn("Отказано в доступе к операции: пользователь {} -> функция {}",
                    currentUser.getUsername(), functionId);
            throw new AccessDeniedException("Нет прав доступа к функции для операции");
        }
    }


     // Проверить, может ли пользователь просматривать функцию (владелец, ADMIN или функция публичная)

    public boolean canViewFunction(Long functionId) {
        User currentUser = getCurrentUser();

        if ("ADMIN".equals(currentUser.getRole())) {
            return true;
        }

        MathFunction function = functionRepository.findById(functionId)
                .orElseThrow(() -> new AccessDeniedException("Функция не найдена"));

        // Владелец может всегда просматривать
        if (function.getOwner().getUserId().equals(currentUser.getUserId())) {
            return true;
        }

        // Другие пользователи могут просматривать только публичные функции
        if (Boolean.TRUE.equals(function.getIsPublic())) {
            return true;
        }

        logger.warn("Отказано в просмотре: пользователь {} -> функция {} (не публичная)",
                currentUser.getUsername(), functionId);
        return false;
    }
}
