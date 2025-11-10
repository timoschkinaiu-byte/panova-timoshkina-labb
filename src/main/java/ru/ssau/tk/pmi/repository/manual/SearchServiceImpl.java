package ru.ssau.tk.pmi.repository.manual;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ssau.tk.pmi.dto.*;

import java.sql.*;
import java.util.*;

public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final Connection connection;
    private final FunctionDao functionDao;

    public SearchServiceImpl(Connection connection, FunctionDao functionDao) {
        this.connection = connection;
        this.functionDao = functionDao;
    }

    @Override
    public <T> List<T> searchByField(Class<T> dtoClass, String fieldName, Object value) {
        logger.info("Searching {} where {} = {}", dtoClass.getSimpleName(), fieldName, value);
        List<Map<String, Object>> results = new ArrayList<>();

        String table = getTableName(dtoClass);
        String query = "SELECT * FROM " + table + " WHERE " + fieldName + " = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(resultSetToMap(rs));
            }
        } catch (SQLException e) {
            logger.error("Error during searchByField", e);
        }

        List<T> dtos = mapResults(dtoClass, results);
        logger.info("Found {} results", dtos.size());
        return dtos;
    }

    @Override
    public <T> List<T> searchByFields(Class<T> dtoClass, Map<String, Object> filters) {
        logger.info("Searching {} with filters {}", dtoClass.getSimpleName(), filters);
        List<Map<String, Object>> results = new ArrayList<>();
        if (filters.isEmpty()) return new ArrayList<>();

        String table = getTableName(dtoClass);
        StringBuilder query = new StringBuilder("SELECT * FROM ").append(table).append(" WHERE ");
        List<Object> values = new ArrayList<>();
        filters.forEach((k, v) -> {
            query.append(k).append(" = ? AND ");
            values.add(v);
        });
        query.setLength(query.length() - 5); // remove last AND

        try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < values.size(); i++) stmt.setObject(i + 1, values.get(i));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) results.add(resultSetToMap(rs));
        } catch (SQLException e) {
            logger.error("Error during searchByFields", e);
        }

        List<T> dtos = mapResults(dtoClass, results);
        logger.info("Found {} results", dtos.size());
        return dtos;
    }

    @Override
    public <T> List<T> searchByFieldSorted(Class<T> dtoClass, String fieldName, Object value, String sortBy, boolean ascending) {
        logger.info("Searching {} where {} = {} sorted by {} {}", dtoClass.getSimpleName(), fieldName, value, sortBy, ascending ? "ASC" : "DESC");
        List<Map<String, Object>> results = new ArrayList<>();

        String table = getTableName(dtoClass);
        String query = "SELECT * FROM " + table + " WHERE " + fieldName + " = ? ORDER BY " + sortBy + (ascending ? " ASC" : " DESC");

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) results.add(resultSetToMap(rs));
        } catch (SQLException e) {
            logger.error("Error during searchByFieldSorted", e);
        }

        List<T> dtos = mapResults(dtoClass, results);
        logger.info("Found {} results", dtos.size());
        return dtos;
    }

    // Поиск по иерархии DFS
    @Override
    public List<FunctionDto> dfsFunctionHierarchy(Long rootFunctionId) {
        logger.info("Starting DFS from function {}", rootFunctionId);
        List<FunctionDto> visited = new ArrayList<>();
        dfsHelper(rootFunctionId, visited);
        logger.info("DFS completed, found {} functions", visited.size());
        return visited;
    }

    private void dfsHelper(Long functionId, List<FunctionDto> visited) {
        FunctionDto f = DtoMapper.mapToFunctionDto(functionDao.getFunctionById(functionId));
        if (f == null || visited.contains(f)) return;
        visited.add(f);
        List<Map<String, Object>> children = functionDao.getAllFunctions(); // допустим, все функции — потенциальные потомки
        for (Map<String, Object> c : children) {
            if (c.get("owner_id").equals(functionId)) {
                dfsHelper((Long)c.get("function_id"), visited);
            }
        }
    }

    // BFS
    @Override
    public List<FunctionDto> bfsFunctionHierarchy(Long rootFunctionId) {
        logger.info("Starting BFS from function {}", rootFunctionId);
        List<FunctionDto> visited = new ArrayList<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(rootFunctionId);

        while (!queue.isEmpty()) {
            Long id = queue.poll();
            FunctionDto f = DtoMapper.mapToFunctionDto(functionDao.getFunctionById(id));
            if (f == null || visited.contains(f)) continue;
            visited.add(f);

            List<Map<String, Object>> children = functionDao.getAllFunctions();
            for (Map<String, Object> c : children) {
                if (c.get("owner_id").equals(id)) queue.add((Long)c.get("function_id"));
            }
        }

        logger.info("BFS completed, found {} functions", visited.size());
        return visited;
    }

    // --- Вспомогательные методы ---
    private String getTableName(Class<?> dtoClass) {
        if (dtoClass == UserDto.class) return "users";
        if (dtoClass == FunctionDto.class) return "functions";
        if (dtoClass == ComputedPointDto.class) return "computed_points";
        if (dtoClass == FunctionAccessDto.class) return "functions_access";
        throw new IllegalArgumentException("Unknown DTO class: " + dtoClass);
    }

    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Map<String, Object> map = new HashMap<>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            map.put(md.getColumnLabel(i), rs.getObject(i));
        }
        return map;
    }

    private <T> List<T> mapResults(Class<T> dtoClass, List<Map<String, Object>> results) {
        List<T> dtos = new ArrayList<>();
        for (Map<String, Object> r : results) {
            if (dtoClass == UserDto.class) dtos.add(dtoClass.cast(DtoMapper.mapToUserDto(r)));
            else if (dtoClass == FunctionDto.class) dtos.add(dtoClass.cast(DtoMapper.mapToFunctionDto(r)));
            else if (dtoClass == ComputedPointDto.class) dtos.add(dtoClass.cast(DtoMapper.mapToComputedPointDto(r)));
            else if (dtoClass == FunctionAccessDto.class) dtos.add(dtoClass.cast(DtoMapper.mapToAccessDto(r)));
        }
        return dtos;
    }
}

