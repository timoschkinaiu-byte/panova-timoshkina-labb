package ru.ssau.tk.pmi.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.entity.MathFunction;
import ru.ssau.tk.pmi.entity.ComputedPoint;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser1 = new User("user1", "hash1", "USER");
        testUser1.setUserId(1L);

        testUser2 = new User("user2", "hash2", "ADMIN");
        testUser2.setUserId(2L);
    }

    @Test
    void testFindByUsername() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(testUser1));
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<User> result1 = userRepository.findByUsername("user1");
        Optional<User> result2 = userRepository.findByUsername("unknown");

        assertTrue(result1.isPresent());
        assertEquals("user1", result1.get().getUsername());
        assertFalse(result2.isPresent());
        verify(userRepository, times(1)).findByUsername("user1");
    }

    @Test
    void testFindByRole() {
        List<User> users = List.of(testUser1);
        when(userRepository.findByRole("USER")).thenReturn(users);

        List<User> result = userRepository.findByRole("USER");

        assertEquals(1, result.size());
        assertEquals("USER", result.get(0).getRole());
        verify(userRepository, times(1)).findByRole("USER");
    }

    @Test
    void testExistsByUsername() {
        when(userRepository.existsByUsername("user1")).thenReturn(true);
        when(userRepository.existsByUsername("unknown")).thenReturn(false);

        boolean exists1 = userRepository.existsByUsername("user1");
        boolean exists2 = userRepository.existsByUsername("unknown");

        assertTrue(exists1);
        assertFalse(exists2);
        verify(userRepository, times(1)).existsByUsername("user1");
        verify(userRepository, times(1)).existsByUsername("unknown");
    }

    @Test
    void testFindByUsernameContaining() {
        List<User> users = List.of(testUser1, testUser2);
        when(userRepository.findByUsernameContaining("user")).thenReturn(users);

        List<User> result = userRepository.findByUsernameContaining("user");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(user -> user.getUsername().contains("user")));
        verify(userRepository, times(1)).findByUsernameContaining("user");
    }

    @Test
    void testFindUsersWithMoreThanNFunctions() {
        List<User> users = List.of(testUser2);
        when(userRepository.findUsersWithMoreThanNFunctions(5)).thenReturn(users);

        List<User> result = userRepository.findUsersWithMoreThanNFunctions(5);

        assertEquals(1, result.size());
        assertEquals("user2", result.get(0).getUsername());
        verify(userRepository, times(1)).findUsersWithMoreThanNFunctions(5);
    }
}