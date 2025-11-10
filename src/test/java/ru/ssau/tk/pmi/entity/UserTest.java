
package ru.ssau.tk.pmi.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserEntityCreation() {
        // Arrange & Act
        User user = new User("test_user", "password_hash", "ADMIN");

        // Assert
        assertNotNull(user);
        assertEquals("test_user", user.getUsername());
        assertEquals("password_hash", user.getPasswordHash());
        assertEquals("ADMIN", user.getRole());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void testUserEntitySetters() {
        // Arrange
        User user = new User();

        // Act
        user.setUserId(1L);
        user.setUsername("alice");
        user.setPasswordHash("new_hash");
        user.setRole("USER");

        // Assert
        assertEquals(1L, user.getUserId());
        assertEquals("alice", user.getUsername());
        assertEquals("new_hash", user.getPasswordHash());
        assertEquals("USER", user.getRole());
    }
}