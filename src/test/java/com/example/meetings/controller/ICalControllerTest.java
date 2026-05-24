package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.ICalService;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
 
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

@Tag("integration")
@WebMvcTest(ICalController.class)
@Import(SecurityConfig.class)
public class ICalControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserService userService;
    @MockBean private UserRepository userRepository;
    @MockBean private MeetingService meetingService;
    @MockBean private ICalService iCalService;
 
   
    /**
     * Test GET /ical/{token}.ics with a valid token returns 200 with text/calendar content
     * and a well-formed VCALENDAR body
     */
    @Test
    void getIcal_TokenIsValid() throws Exception {
        User user = new User("nuno", "nuno@gmail.pt", "password");

        when(userRepository.findByIcalToken(user.getIcalToken()))
                .thenReturn(Optional.of(user));

        when(meetingService.calendarFor(user))
                .thenReturn(java.util.Collections.emptyList());

        when(iCalService.render(user, java.util.Collections.emptyList()))
                .thenReturn("BEGIN:VCALENDAR\nEND:VCALENDAR");

        mockMvc.perform(get("/ical/" + user.getIcalToken() + ".ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andExpect(content().string(containsString("BEGIN:VCALENDAR")))
                .andExpect(content().string(containsString("END:VCALENDAR")));
    }

    /**
     * Test GET /ical/{token}.ics with an invalid token returns 404
     */
    @Test
    void getIcal_TokenIsInvalid() throws Exception {
        mockMvc.perform(get("/ical/invalid-token.ics"))
                .andExpect(status().isNotFound());
    }
    
}
