package com.example.meetings.UnitTestsBusinessLogic;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
 
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Unit tests for UserService class
 * Validates the user management business logic, ensuring that 
 * registration and retrieval operations strictly follow application rules
 * 
 * 
 * Criteria: Line and Branch Coverage
 * Goal: 100%
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
 
    @Mock
    private PasswordEncoder passwordEncoder;
 
    @InjectMocks
    private UserService userService;
 
    private User user;
 
    @BeforeEach
    void setUp() {
        user = new User("nuno", "nuno@gmail.pt", "hash-123");
    }

    /**
     * Tests that registering a user with an already taken username
     * throws an IllegalArgumentException, "Username already taken"
     */
    @Test
    void register_AlreadyExistingUsername() {
        when(userRepository.existsByUsername("nuno")).thenReturn(true);
 
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register("nuno", "nuno@gmail.pt", "123"));
 
        assertEquals("Username already taken", ex.getMessage());

        // Verifies that the save() method was never called with a User object
        verify(userRepository, never()).save(any(User.class));
        // Verifies that the encode() method was never called with a String
        verify(passwordEncoder, never()).encode(anyString());
    }

    /**
     * Tests that registering with a new username encodes the password
     * and saves the user correctly
     */
    @Test
    void register_Correct() {
        when(userRepository.existsByUsername("nuno")).thenReturn(false);
        when(passwordEncoder.encode("123")).thenReturn("hash-123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
 
        User result = userService.register("nuno", "nuno@gmail.pt", "123");
 
        assertNotNull(result);
        assertEquals("nuno", result.getUsername());
        assertEquals("nuno@gmail.pt", result.getEmail());
        assertEquals("hash-123", result.getPasswordHash());
 
        verify(passwordEncoder).encode("123");
        verify(userRepository).save(any(User.class));
    }



    /**
     * Tests that the function throws an IllegalArgumentException, "Unknown user: " + username
     * when a user does not exist
     */
    @Test
    void requireByUsername_UserNotExist() {
        when(userRepository.findByUsername("unkown")).thenReturn(Optional.empty());
 
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.requireByUsername("unkown"));
 
        assertEquals("Unknown user: unkown", ex.getMessage());
    }

    /**
     * Tests that returns the correct user when it exists
     */
    @Test
    void requireByUsername_ExistingUser() {
        when(userRepository.findByUsername("nuno")).thenReturn(Optional.of(user));
 
        User result = userService.requireByUsername("nuno");
 
        assertSame(user, result);
        verify(userRepository).findByUsername("nuno");
    }
    
}
