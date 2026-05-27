package com.example.meetings.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.service.UserService;

/**
 * Integration tests for AuthController
 */
@Tag("integration")
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private UserService userService;

    /**
     * Test GET /login returns 200 and renders the login page
     */
    @Test
    void getLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    /**
     * Test GET /register returns 200 and renders the registration form
     */
    @Test
    void getRegister() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    /**
     * Test POST /register with an already taken username re-renders the form with an error
     */
    @Test
    void postRegister_UsernameIsTaken() throws Exception {

        when(userService.register(eq("nuno"), anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Username already taken"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "nuno")
                        .param("email", "not-nuno@gmail.pt")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Username already taken")));
    }

    /**
     * Test POST /register with a new username redirects to /login?registered
     */
    @Test
    void postRegister_UsernameIsNew() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                .param("username", "new-user")
                .param("email", "new-user@email.pt")
                .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }


    /**
     * Test GET / redirects to /calendar
     */
    @Test
    void getRoot() throws Exception {
        mockMvc.perform(get("/").with(user("nuno").password("password").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }
 
    
}
