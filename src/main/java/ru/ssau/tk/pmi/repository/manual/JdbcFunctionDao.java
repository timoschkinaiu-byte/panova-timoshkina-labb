package ru.ssau.tk.pmi.repository.manual;

import java.sql.*;
import java.util.*;

public class JdbcFunctionDao implements FunctionDao {
    private final Connection connection;

    public JdbcFunctionDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Long insertFunction(String functionName, String functionDefinition, Long ownerId, boolean isPublic) {
        String sql = "INSERT INTO functions (function_name, function_definition, owner_id, is_public) VALUES (?, ?, ?, ?) RETURNING function_id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, functionName);
            stmt.setString(2, functionDefinition);
            stmt.setLong(3, ownerId);
            stmt.setBoolean(4, isPublic);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("function_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map<String, Object> getFunctionById(Long id) {
        String sql = "SELECT * FROM functions WHERE function_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getAllFunctions() {
        List<Map<String, Object>> functions = new ArrayList<>();
        String sql = "SELECT * FROM functions";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                functions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return functions;
    }

    @Override
    public void updateFunction(Long id, String name, String definition, boolean isPublic) {
        String sql = "UPDATE functions SET function_name = ?, function_definition = ?, is_public = ? WHERE function_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, definition);
            stmt.setBoolean(3, isPublic);
            stmt.setLong(4, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteFunction(Long id) {
        String sql = "DELETE FROM functions WHERE function_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("function_id", rs.getLong("function_id"));
        map.put("function_name", rs.getString("function_name"));
        map.put("function_definition", rs.getString("function_definition"));
        map.put("owner_id", rs.getLong("owner_id"));
        map.put("is_public", rs.getBoolean("is_public"));
        return map;
    }
}


