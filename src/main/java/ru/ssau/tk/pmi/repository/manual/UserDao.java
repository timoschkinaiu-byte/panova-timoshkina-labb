package ru.ssau.tk.pmi.repository.manual;

import java.util.List;
import java.util.Map;

public interface UserDao {
    void insertUser(String username, String passwordHash, String role, Boolean enabled);
    Map<String, Object> getUserById(Long id);
    Map<String, Object> getUserByUsername(String username);
    void updateUser(Long id, String username, String passwordHash, String role, Boolean enabled);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    void updateEnabledStatus(Long userId, Boolean enabled);
    void updateUserRole(Long userId, String role);
}

