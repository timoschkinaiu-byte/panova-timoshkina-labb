package ru.ssau.tk.pmi.repository.manual;

import java.sql.*;
import java.util.*;

public class JdbcUserDao implements UserDao {
    private final Connection connection;

    public JdbcUserDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insertUser(String username, String passwordHash, String role, Boolean enabled) {
        String sql = "INSERT INTO users (username, password_hash, role, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.setBoolean(4, enabled != null ? enabled : true);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting user", e);
        }
    }

    @Override
    public Map<String, Object> getUserById(Long id) {
        String sql = "SELECT user_id, username, password_hash, role, enabled, created_at, updated_at FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching user", e);
        }
        return null;
    }

    @Override
    public Map<String, Object> getUserByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash, role, enabled, created_at, updated_at FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null; // Просто логируем ошибку
        }
        return null;
    }

    @Override
    public void updateUser(Long id, String username, String passwordHash, String role, Boolean enabled) {
        String sql = "UPDATE users SET username = ?, password_hash = ?, role = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.setBoolean(4, enabled != null ? enabled : true);
            stmt.setLong(5, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user", e);
        }
    }

    @Override
    public void deleteUser(Long id) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking username existence", e);
        }
        return false;
    }

    @Override
    public void updateEnabledStatus(Long userId, Boolean enabled) {
        String sql = "UPDATE users SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, enabled != null ? enabled : true);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating enabled status", e);
        }
    }

    @Override
    public void updateUserRole(Long userId, String role) {
        String sql = "UPDATE users SET role = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user role", e);
        }
    }

    private Map<String, Object> extractUserFromResultSet(ResultSet rs) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", rs.getLong("user_id"));
        map.put("username", rs.getString("username"));
        map.put("password_hash", rs.getString("password_hash"));
        map.put("role", rs.getString("role"));
        map.put("enabled", rs.getBoolean("enabled"));
        map.put("created_at", rs.getTimestamp("created_at"));
        map.put("updated_at", rs.getTimestamp("updated_at"));
        return map;
    }
}