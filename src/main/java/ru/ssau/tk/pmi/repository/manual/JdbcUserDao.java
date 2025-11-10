package ru.ssau.tk.pmi.repository.manual;

import java.sql.*;
import java.util.*;

public class JdbcUserDao implements UserDao {
    private final Connection connection;

    public JdbcUserDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insertUser(String username, String passwordHash, String role) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting user", e);
        }
    }

    @Override
    public Map<String, Object> getUserById(Long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("user_id", rs.getLong("user_id"));
                map.put("username", rs.getString("username"));
                map.put("password_hash", rs.getString("password_hash"));
                map.put("role", rs.getString("role"));
                return map;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching user", e);
        }
        return null;
    }

    @Override
    public Map<String, Object> getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("user_id", rs.getLong("user_id"));
                map.put("username", rs.getString("username"));
                map.put("password_hash", rs.getString("password_hash"));
                map.put("role", rs.getString("role"));
                return map;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching user by username", e);
        }
        return null;
    }

    @Override
    public void updateUser(Long id, String username, String passwordHash, String role) {
        String sql = "UPDATE users SET username = ?, password_hash = ?, role = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.setLong(4, id);
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
}



