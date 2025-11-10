package ru.ssau.tk.pmi.repository.manual;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

public class JdbcComputedPointDao implements ComputedPointDao {
    private static final Logger logger = LogManager.getLogger(JdbcComputedPointDao.class);
    private final Connection connection;
    public JdbcComputedPointDao(Connection connection) {
        this.connection = connection;
    }
    @Override
    public Long insertComputedPoint(Long functionId, double xValue, double yValue) {
        String sql = "INSERT INTO computed_points (function_id, x_value, y_value) VALUES (?, ?, ?) RETURNING point_id";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, functionId);
            stmt.setDouble(2, xValue);
            stmt.setDouble(3, yValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                logger.info("Inserted computed point with ID={}", id);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error inserting computed point", e);
        }
        return null;
    }

    @Override
    public Map<String, Object> getComputedPointById(Long id) {
        String sql = "SELECT * FROM computed_points WHERE point_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return extractRow(rs);
            }
        } catch (SQLException e) {
            logger.error("Error fetching computed point by id={}", id, e);
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> getComputedPointsByFunctionId(Long functionId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM computed_points WHERE function_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, functionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching computed points by functionId={}", functionId, e);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getAllComputedPoints() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM computed_points";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all computed points", e);
        }
        return result;
    }

    @Override
    public void updateComputedPoint(Long id, double xValue, double yValue) {
        String sql = "UPDATE computed_points SET x_value = ?, y_value = ? WHERE point_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, xValue);
            stmt.setDouble(2, yValue);
            stmt.setLong(3, id);
            stmt.executeUpdate();
            logger.info("Updated computed point id={}", id);
        } catch (SQLException e) {
            logger.error("Error updating computed point id={}", id, e);
        }
    }
    @Override
    public void deleteComputedPoint(Long id) {
        String sql = "DELETE FROM computed_points WHERE point_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
            logger.info("Deleted computed point id={}", id);
        } catch (SQLException e) {
            logger.error("Error deleting computed point id={}", id, e);
        }
    }
    private Map<String, Object> extractRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("point_id", rs.getLong("point_id"));
        row.put("function_id", rs.getLong("function_id"));
        row.put("x_value", rs.getDouble("x_value"));
        row.put("y_value", rs.getDouble("y_value"));
        return row;
    }
}

