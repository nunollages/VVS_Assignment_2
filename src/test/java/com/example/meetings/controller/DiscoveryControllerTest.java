package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.discover.DiscoveryService;
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
 
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DiscoveryController
 */
@Tag("integration")
@WebMvcTest(DiscoveryController.class)
@Import(SecurityConfig.class)
public class DiscoveryControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DiscoveryService discoveryService;
    @MockBean private MeetingService meetingService;
    @MockBean private UserService userService;

    /**
     * Test GET /discover without a query parameter returns 200
     */
    @Test
    @WithMockUser
    void getDiscover_NoQueryParameter() throws Exception {
        // The page should still render without any errors
        mockMvc.perform(get("/discover"))
                .andExpect(status().isOk());
    }

    /**
     * Test GET /discover with a query parameter returns 200
     */
    @Test
    @WithMockUser
    void getDiscover_QueryParameter() throws Exception {
        mockMvc.perform(get("/discover").param("q", "concert"))
                .andExpect(status().isOk());
    }

    /**
     * Test POST /discover/copy with valid params copies the event and redirects to /calendar
     */
    @Test
    @WithMockUser
    void postDiscover_ValidParams() throws Exception {
        // Copy a discovered event into the user's calendar
        mockMvc.perform(post("/discover/copy").with(csrf())
                        .param("source", "ticketmaster")
                        .param("externalId", "tm-001")
                        .param("title", "Drake Iceman")
                        .param("start", "2027-06-15T20:00:00Z")
                        .param("venue", "Altice Arena"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }
 
    
}
