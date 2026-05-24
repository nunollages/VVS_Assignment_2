package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@WebMvcTest(CalendarController.class)
@Import(SecurityConfig.class)
public class CalendarControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MeetingService meetingService;
    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;

    /**
     * Test GET /calendar without authentication redirects to login
     */
    @Test
    void getCalendar_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/calendar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * Test GET /calendar with a valid authenticated user
     */
    @Test
    @WithMockUser(username = "nuno")
    void getCalendar_WhenAuthenticated() throws Exception {
        User mockUser = new User("nuno", "nuno@example.com", "password");
        when(userService.requireByUsername("nuno")).thenReturn(mockUser);

        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"));
    }
}