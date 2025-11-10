package ru.ssau.tk.pmi.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.ComputedPoint;
import ru.ssau.tk.pmi.repository.UserRepository;
import ru.ssau.tk.pmi.repository.MathFunctionRepository;
import ru.ssau.tk.pmi.repository.ComputedPointRepository;

import java.util.*;

@Service
@Transactional
public class springSearchService {
    private static final Logger logger = LogManager.getLogger(springSearchService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MathFunctionRepository mathFunctionRepository;

    @Autowired
    private ComputedPointRepository computedPointRepository;

    // 1. ПОИСК В ГЛУБИНУ (Depth-First Search) по иерархии
    public List<Object> depthFirstSearch(Long startUserId, String target) {
        logger.info("Запуск поиска в глубину для пользователя ID: {}, цель: {}", startUserId, target);
        List<Object> results = new ArrayList<>();
        Set<Long> visitedUsers = new HashSet<>();
        Set<Long> visitedFunctions = new HashSet<>();

        Optional<User> startUser = userRepository.findById(startUserId);
        if (startUser.isPresent()) {
            depthFirstSearchRecursive(startUser.get(), target, visitedUsers, visitedFunctions, results);
        }

        logger.info("Поиск в глубину завершен. Найдено результатов: {}", results.size());
        return results;
    }

    private void depthFirstSearchRecursive(User user, String target,
                                           Set<Long> visitedUsers, Set<Long> visitedFunctions,
                                           List<Object> results) {
        if (visitedUsers.contains(user.getUserId())) {
            return;
        }
        visitedUsers.add(user.getUserId());

        logger.debug("Обработка пользователя: {}", user.getUsername());

        // Поиск в данных пользователя
        searchInUserData(user, target, results);

        // Рекурсивный поиск в функциях пользователя (LAZY loading)
        List<MathFunction> userFunctions = user.getFunctions();
        for (MathFunction function : userFunctions) {
            if (!visitedFunctions.contains(function.getFunctionId())) {
                visitedFunctions.add(function.getFunctionId());
                searchInFunctionData(function, target, results);

                // Рекурсивный поиск в точках функции
                List<ComputedPoint> functionPoints = function.getComputedPoints();
                for (ComputedPoint point : functionPoints) {
                    searchInPointData(point, target, results);
                }
            }
        }
    }

    // 2. ПОИСК В ШИРИНУ (Breadth-First Search)
    public List<Object> breadthFirstSearch(Long startUserId, String target) {
        logger.info("Запуск поиска в ширину для пользователя ID: {}, цель: {}", startUserId, target);
        List<Object> results = new ArrayList<>();
        Queue<User> userQueue = new LinkedList<>();
        Set<Long> visitedUsers = new HashSet<>();
        Set<Long> visitedFunctions = new HashSet<>();

        Optional<User> startUser = userRepository.findById(startUserId);
        if (startUser.isPresent()) {
            userQueue.offer(startUser.get());
            visitedUsers.add(startUser.get().getUserId());
        }

        while (!userQueue.isEmpty()) {
            User currentUser = userQueue.poll();
            logger.debug("Обработка пользователя из очереди: {}", currentUser.getUsername());

            // Поиск в данных текущего пользователя
            searchInUserData(currentUser, target, results);

            // Обработка функций пользователя
            List<MathFunction> userFunctions = currentUser.getFunctions();
            for (MathFunction function : userFunctions) {
                if (!visitedFunctions.contains(function.getFunctionId())) {
                    visitedFunctions.add(function.getFunctionId());
                    searchInFunctionData(function, target, results);

                    // Обработка точек функции
                    List<ComputedPoint> functionPoints = function.getComputedPoints();
                    for (ComputedPoint point : functionPoints) {
                        searchInPointData(point, target, results);
                    }
                }
            }
        }

        logger.info("Поиск в ширину завершен. Найдено результатов: {}", results.size());
        return results;
    }

    // 3. ПОИСК ПО ИЕРАРХИИ
    public Map<String, List<Object>> hierarchicalSearch(Long userId, String target) {
        logger.info("Запуск поиска по иерархии для пользователя ID: {}, цель: {}", userId, target);
        Map<String, List<Object>> hierarchicalResults = new HashMap<>();

        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            // Поиск на уровне пользователя
            List<Object> userLevelResults = new ArrayList<>();
            searchInUserData(user.get(), target, userLevelResults);
            hierarchicalResults.put("USERS", userLevelResults);

            // Поиск на уровне функций
            List<Object> functionLevelResults = new ArrayList<>();
            List<MathFunction> userFunctions = user.get().getFunctions();
            for (MathFunction function : userFunctions) {
                searchInFunctionData(function, target, functionLevelResults);
            }
            hierarchicalResults.put("FUNCTIONS", functionLevelResults);

            // Поиск на уровне точек
            List<Object> pointLevelResults = new ArrayList<>();
            for (MathFunction function : userFunctions) {
                List<ComputedPoint> functionPoints = function.getComputedPoints();
                for (ComputedPoint point : functionPoints) {
                    searchInPointData(point, target, pointLevelResults);
                }
            }
            hierarchicalResults.put("POINTS", pointLevelResults);
        }

        logger.info("Поиск по иерархии завершен. Результаты: {}", hierarchicalResults.keySet());
        return hierarchicalResults;
    }

    // 4. ОДИНОЧНЫЙ ПОИСК с использованием Spring Data методов
    public List<Object> singleSearch(String field, String value) {
        logger.info("Одиночный поиск: поле={}, значение={}", field, value);
        List<Object> results = new ArrayList<>();

        switch (field.toUpperCase()) {
            case "USERNAME":
                userRepository.findByUsernameContainingIgnoreCase(value).forEach(results::add);
                break;
            case "ROLE":
                userRepository.findByRole(value).forEach(results::add);
                break;
            case "FUNCTION_NAME":
                mathFunctionRepository.findByFunctionNameContainingIgnoreCase(value).forEach(results::add);
                break;
            case "FUNCTION_TYPE":
                mathFunctionRepository.findByFunctionType(value).forEach(results::add);
                break;
            case "IS_PUBLIC":
                mathFunctionRepository.findByIsPublicTrue().forEach(results::add);
                break;
            case "X_VALUE":
                // Для поиска по точкам нужна функция
                break;
            default:
                logger.warn("Неизвестное поле для поиска: {}", field);
        }

        logger.info("Одиночный поиск завершен. Найдено результатов: {}", results.size());
        return results;
    }

    // 5. МНОЖЕСТВЕННЫЙ ПОИСК
    public List<Object> multipleSearch(Map<String, String> criteria) {
        logger.info("Множественный поиск по критериям: {}", criteria);
        Set<Object> results = new HashSet<>();

        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            List<Object> singleResults = singleSearch(entry.getKey(), entry.getValue());
            results.addAll(singleResults);
        }

        logger.info("Множественный поиск завершен. Найдено уникальных результатов: {}", results.size());
        return new ArrayList<>(results);
    }

    // 6. ПОИСК С СОРТИРОВКОЙ используя Spring Data Sort
    public List<Object> searchWithSorting(String field, String value, String sortBy, boolean ascending) {
        logger.info("Поиск с сортировкой: поле={}, значение={}, сортировка по={}, порядок={}",
                field, value, sortBy, ascending ? "ASC" : "DESC");

        List<Object> results = new ArrayList<>();
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        switch (field.toUpperCase()) {
            case "USERNAME":
                results.addAll(userRepository.findByUsernameContainingIgnoreCase(value, sort));
                break;
            case "FUNCTION_NAME":
                results.addAll(mathFunctionRepository.findByFunctionNameContainingIgnoreCase(value, sort));
                break;
            case "FUNCTION_TYPE":
                results.addAll(mathFunctionRepository.findByFunctionType(value, sort));
                break;
            default:
                results = singleSearch(field, value);
        }

        logger.info("Поиск с сортировкой завершен. Отсортировано результатов: {}", results.size());
        return results;
    }

    // 7. РАСШИРЕННЫЙ ПОИСК с использованием @Query методов
    public List<MathFunction> findFunctionsWithMinPoints(int minPoints) {
        logger.info("Расширенный поиск: функции с минимум {} точек", minPoints);
        List<MathFunction> functions = mathFunctionRepository.findFunctionsWithMinPoints(minPoints);
        logger.info("Найдено функций с {}+ точками: {}", minPoints, functions.size());
        return functions;
    }

    public List<User> findUsersWithMoreThanNFunctions(int minFunctions) {
        logger.info("Поиск пользователей с {}+ функциями", minFunctions);
        List<User> users = userRepository.findUsersWithMoreThanNFunctions(minFunctions);
        logger.info("Найдено пользователей с {}+ функциями: {}", minFunctions, users.size());
        return users;
    }

    // Вспомогательные методы для поиска в данных
    private void searchInUserData(User user, String target, List<Object> results) {
        if (user.getUsername().toLowerCase().contains(target.toLowerCase()) ||
                user.getRole().toLowerCase().contains(target.toLowerCase())) {
            results.add(user);
            logger.debug("Пользователь найден: {}", user.getUsername());
        }
    }

    private void searchInFunctionData(MathFunction function, String target, List<Object> results) {
        if (function.getFunctionName().toLowerCase().contains(target.toLowerCase()) ||
                function.getFunctionDefinition().toLowerCase().contains(target.toLowerCase()) ||
                function.getFunctionType().toLowerCase().contains(target.toLowerCase())) {
            results.add(function);
            logger.debug("Функция найдена: {}", function.getFunctionName());
        }
    }

    private void searchInPointData(ComputedPoint point, String target, List<Object> results) {
        if (String.valueOf(point.getXValue()).contains(target) ||
                String.valueOf(point.getYValue()).contains(target)) {
            results.add(point);
            logger.debug("Точка найдена: x={}, y={}", point.getXValue(), point.getYValue());
        }
    }
}
