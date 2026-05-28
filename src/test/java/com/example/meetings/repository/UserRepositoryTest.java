package com.example.meetings.repository;

import com.example.meetings.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
 
import java.util.Optional;
 
import static org.junit.jupiter.api.Assertions.*;

/**
 * Database integration tests for UserRepository
 */
@Tag("integration")
@DataJpaTest
public class UserRepositoryTest {

    @Autowired private UserRepository userRepository;

    private User nuno;

    @BeforeEach
    void setUp() {
        nuno = userRepository.save(new User("nuno", "nuno@gmail.pt", "hashed123"));
    }

    /**
     * Test if it returns the correct user when the username exists in the databse
     */
    @Test
    void findByUsername_UsernameExists() {
        Optional<User> result = userRepository.findByUsername("nuno");
 
        assertTrue(result.isPresent());
        assertEquals("nuno", result.get().getUsername());
        assertEquals("nuno@gmail.pt", result.get().getEmail());
    }

    /**
     * Test if it returns empty when the username does not exist
     */
    @Test
    void findByUsername_UsernameDoesNotExist() {
        Optional<User> result = userRepository.findByUsername("unkown");
 
        assertTrue(result.isEmpty());
    }

    /**
     * Test if it returns the correct user when the token exists
     */
    @Test
    void findByIcal_TokenTokenExists() {
        Optional<User> result = userRepository.findByIcalToken(nuno.getIcalToken());
 
        assertTrue(result.isPresent());
        assertEquals("nuno", result.get().getUsername());
    }

    /**
     * Test if it returns empty when the token does not exist
     */
    @Test
    void findByIcalToken_TokenDoesNotExist() {
        Optional<User> result = userRepository.findByIcalToken("invalid-token");
 
        assertTrue(result.isEmpty());
    }

    /**
     * Test if it returns true when the username exists
     */
    @Test
    void existsByUsername_UsernameExists() {
        assertTrue(userRepository.existsByUsername("nuno"));
    }

    /**
     * Test if it retuns false when the username does not exist
     */
    @Test
    void existsByUsername_UsernameDoesNotExist() {
        assertFalse(userRepository.existsByUsername("ghost"));
    }
 


    
}
