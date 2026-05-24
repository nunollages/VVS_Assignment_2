package com.example.meetings.controller;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.User;
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
 
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@WebMvcTest(MeetingController.class)
@Import(SecurityConfig.class)
public class MeetingControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MeetingService meetingService;
    @MockBean private UserService userService;

    /**
     * Test GET /meetings/new returns 200 and renders the propose form
     */
    @Test
    @WithMockUser
    void getMeetingsNew_Authenticated() throws Exception {

        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().isOk());
    }

    /**
     * Test POST /meetings/new with valid data redirects to /calendar
     */
    @Test
    @WithMockUser(username = "nuno", roles = "USER")
    void postMeetingsNew_DataIsValid() throws Exception {

        User organizer = new User("nuno", "nuno@gmail.com", "hashed-password");

        org.mockito.Mockito.when(userService.requireByUsername("nuno"))
                .thenReturn(organizer);

        mockMvc.perform(post("/meetings/new")
                        .with(csrf())
                        .param("title", "Drake Iceman")
                        .param("description", "Drake Worldtour")
                        .param("start", "2027-06-15T09:00")
                        .param("end", "2027-06-15T09:30")
                        .param("invitees", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        verify(meetingService).propose(
                any(),
                eq("Drake Iceman"),
                eq("Drake Worldtour"),
                any(),
                any(),
                eq(List.of())
        );
    }

    /**
     * Test POST /meetings/new with end before start re-renders the form with an error
     */
    @Test
    @WithMockUser(username = "nuno", roles = "USER")
    void postMeetingsNew_EndBeforeStart() throws Exception {

        User organizer = new User("nuno", "nuno@gmail.com", "hashed-password");

        org.mockito.Mockito.when(userService.requireByUsername("nuno"))
                .thenReturn(organizer);

        doThrow(new RuntimeException("End time must be after start time"))
                .when(meetingService)
                .propose(any(), any(), any(), any(), any(), anyList());

        mockMvc.perform(post("/meetings/new")
                        .with(csrf())
                        .param("title", "Bad Meeting")
                        .param("start", "2027-06-15T10:00")
                        .param("end", "2027-06-15T09:00")
                        .param("invitees", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("End time must be after start time")));
    }

    /**
     * Test POST /meetings/{id}/respond with action=accept redirects to /calendar
     */
    @Test
    @WithMockUser(username = "invitee1", roles = "USER")
    void postMeetingsRespond_Accepting() throws Exception {

        User user = new User("nuno", "nuno@gmail.com", "hashed-password");
        org.mockito.Mockito.when(userService.requireByUsername("invitee1"))
                .thenReturn(user);

        mockMvc.perform(post("/meetings/1/respond")
                        .with(csrf())
                        .param("action", "accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        verify(meetingService).respond(eq(1L), any(), eq(InviteStatus.ACCEPTED));
    }

    /**
     * Test POST /meetings/{id}/respond with action=decline redirects to /calendar
     */
    @Test
    @WithMockUser(username = "invitee1", roles = "USER")
    void postMeetingsRespond_Declining() throws Exception {

        User user = new User("nuno", "nuno@gmail.com", "hashed-password");
        org.mockito.Mockito.when(userService.requireByUsername("invitee1"))
                .thenReturn(user);

        mockMvc.perform(post("/meetings/1/respond")
                        .with(csrf())
                        .param("action", "decline"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        verify(meetingService).respond(eq(1L), any(), eq(InviteStatus.DECLINED));
    }
    
}
