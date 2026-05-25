package com.example.meetings.UnitTestsBusinessLogic;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.AppUserDetailsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
 
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        user = new User("nuno", "nuno@gmail.pt", "hash-123");
    }

    /**
     * Tets that the function throws a UsernameNotFoundException, "Unknown user: " + username
     * when the user does not exist
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
     * when the user exists
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
