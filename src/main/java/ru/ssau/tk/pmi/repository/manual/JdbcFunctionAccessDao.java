package ru.ssau.tk.pmi.repository.manual;

import java.sql.*;
import java.util.*;

public class JdbcFunctionAccessDao implements FunctionAccessDao {
    private final Connection connection;
    public JdbcFunctionAccessDao(Connection connection) {
        this.connection = connection;
    }
    @Override
    public Long insertAccess(Long functionId, Long userId, String accessType) {
        String sql = "INSERT INTO functions_access (function_id, user_id, access_type) VALUES (?, ?, ?) RETURNING access_id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, functionId);
            stmt.setLong(2, userId);
            stmt.setString(3, accessType);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("access_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public Map<String, Object> getAccessById(Long accessId) {
        String sql = "SELECT * FROM functions_access WHERE access_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, accessId);
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
    public List<Map<String, Object>> getAccessByFunctionAndUser(Long functionId, Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM functions_access WHERE function_id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, functionId);
            stmt.setLong(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    @Override
    public List<Map<String, Object>> getAllAccess() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM functions_access";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    @Override
    public void updateAccess(Long accessId, String accessType) {
        String sql = "UPDATE functions_access SET access_type = ? WHERE access_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, accessType);
            stmt.setLong(2, accessId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void deleteAccess(Long accessId) {
        String sql = "DELETE FROM functions_access WHERE access_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, accessId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("access_id", rs.getLong("access_id"));
        map.put("function_id", rs.getLong("function_id"));
        map.put("user_id", rs.getLong("user_id"));
        map.put("access_type", rs.getString("access_type"));
        return map;
    }
}
