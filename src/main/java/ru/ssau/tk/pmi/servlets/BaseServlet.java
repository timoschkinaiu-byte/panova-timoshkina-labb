package ru.ssau.tk.pmi.servlets;

import ru.ssau.tk.pmi.database.DatabaseConnection;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.Map;
import java.util.logging.Logger;

public abstract class BaseServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(BaseServlet.class.getName());

    protected Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    protected String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    protected Long getLongFromObject(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    protected Double getDoubleFromObject(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Integer) return ((Integer) obj).doubleValue();
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    protected Boolean getBooleanFromObject(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof String) return Boolean.parseBoolean((String) obj);
        return null;
    }
    protected Map<String, Object> getAuthenticatedUser(HttpServletRequest request) {
        return (Map<String, Object>) request.getAttribute("authenticatedUser");
    }

    protected boolean hasRole(HttpServletRequest request, String requiredRole) {
        Map<String, Object> user = getAuthenticatedUser(request);
        if (user == null) return false;

        String userRole = (String) user.get("role");
        return requiredRole.equals(userRole);
    }

    protected boolean isResourceOwner(HttpServletRequest request, Long resourceOwnerId) {
        Map<String, Object> user = getAuthenticatedUser(request);
        if (user == null) return false;

        Long userId = (Long) user.get("user_id");
        return userId.equals(resourceOwnerId);
    }

    protected boolean hasAccess(HttpServletRequest request, Long resourceOwnerId) {
        return hasRole(request, "ADMIN") || isResourceOwner(request, resourceOwnerId);
    }

}