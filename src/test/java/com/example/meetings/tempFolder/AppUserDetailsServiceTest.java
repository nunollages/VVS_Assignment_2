package com.example.meetings.unitTestsBusinessLogic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.AppUserDetailsService;

/**
 * Unit tests for AppUserDetailsService class
 * This class validates the business logic for user authentication
 * 
 * Criteria: Line and Branch Coverage
 * Goal: 100%
 */
@ExtendWith(MockitoExtension.class)
public class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;
 
    // Injects the userRepository Mock into the AppUserDetailsService
    @InjectMocks
    private AppUserDetailsService appUserDetailsService;
 
    private User user;
 
    @BeforeEach
    void setUp() {
        // Before each test, a user is created
        user = new User("nuno", "nuno@gmail.pt", "hash-123");
    }

    /**
     * Tets that the function throws a UsernameNotFoundException, "Unknown user: " + username
     * when is called with a username that does not exist
     */
    @Test
    void loadUserByUsername_UserNotExist() {
        // The mock will return empty everytime someone searches for the user "unkown"
        when(userRepository.findByUsername("unkown")).thenReturn(Optional.empty());
 
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> appUserDetailsService.loadUserByUsername("unkown"));
 
        assertEquals("Unknown user: unkown", ex.getMessage());
    }

    /**
     * Tests that the function returns the correct username, passwordHash and ROLE_USER
     * when it's called with an existing username
     */
    @Test
    void loadUserByUsername_ExistingUser() {
         // The mock will return the added user everytime someone searches for the user "nuno"
        when(userRepository.findByUsername("nuno")).thenReturn(Optional.of(user));
 
        UserDetails result = appUserDetailsService.loadUserByUsername("nuno");
 
        assertEquals("nuno", result.getUsername());
        assertEquals("hash-123", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
 
        verify(userRepository).findByUsername("nuno");
    }
    
}
