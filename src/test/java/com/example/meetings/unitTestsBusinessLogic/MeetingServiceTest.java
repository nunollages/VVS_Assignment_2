package com.example.meetings.unitTestsBusinessLogic;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.MeetingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MeetingService class
 * This suite validates the core business logic, including meeting proposal,
 * invitation handling, and integration with external discovery sources
 * 
 * Criteria: Line and Branch Coverage
 * Goal: 100%
 */
@ExtendWith(MockitoExtension.class)
public class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeetingService meetingService;

    private User organizer;
    private User invitee1;
    private User invitee2;

    @BeforeEach
    void setUp() {
        // Before each test, three users are created
        organizer = new User("nuno", "nuno@gmail.pt", "123");
        invitee1 = new User("invitee1", "invitee1@gmail.pt", "123");
        invitee2 = new User("invitee2", "invitee2@gmail.pt", "123");
    }

    /**
     * Tests that creating a meeting with the same start and end time,
     * throws an IllegalArgumentException with the message "End time must be after start time"
     */
    @Test
    void propose_EndTimeEqualToStartTime() {

        Instant now = Instant.now();
        List<String> invitees = List.of("invitee1");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            meetingService.propose(organizer, "Meeting 1", "Start = End", now, now, invitees)
        );

        assertEquals("End time must be after start time", exception.getMessage());
        // Validate that no repository was consulted
        verifyNoInteractions(userRepository, meetingRepository, participantRepository);
    }

    /**
     * Tests that creating a meeting with the end time before the start time
     * throws an IllegalArgumentException with the message "End time must be after start time"
     */
    @Test
    void propose_EndTimeIsBeforeStartTime() {

        Instant start = Instant.now();
        Instant end = start.minusSeconds(60);

        List<String> invitees = List.of("invitee1");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            meetingService.propose(organizer, "Meeting 2", "Start > End", start, end, invitees)
        );

        assertEquals("End time must be after start time", exception.getMessage());
        // Validate that no repository was consulted
        verifyNoInteractions(userRepository, meetingRepository, participantRepository);
    }

    /**
     * Tests that creating a meeting with an unexisting invite guest,
     * throws an IllegalArgumentException with the message "Unknown invitee:" + unkown
     */
    @Test
    void propose_InviteeDoesNotExist() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        List<String> invitees = List.of("invitee1", "unkown");

        when(userRepository.findByUsername("invitee1")).thenReturn(Optional.of(invitee1));
        when(userRepository.findByUsername("unkown")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            meetingService.propose(organizer, "Meeting 3", "Unkown invitee", start, end, invitees)
        );

        assertEquals("Unknown invitee: unkown", exception.getMessage());
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    /**
     * Test if null, duplicate and organizer invitees are silently skipped.
     * Also test if organizer invite status gets automatically ACCEPTED and
     * the other invites have PENDING status
     */
    @Test
    void propose_InputsHaveDuplicatesAndWhitespaces() {

        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        
        List<String> invitees = new ArrayList<>();
        invitees.add("invitee1");
        invitees.add("  invitee1  ");
        invitees.add(null);
        invitees.add("");
        invitees.add("invitee2");
        invitees.add("nuno");

        when(userRepository.findByUsername("invitee1")).thenReturn(Optional.of(invitee1));
        when(userRepository.findByUsername("invitee2")).thenReturn(Optional.of(invitee2));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));
 
        Meeting result = meetingService.propose(organizer, "Meeting 4", "Description", start, end, invitees);
 
        assertNotNull(result);
        assertEquals("Meeting 4", result.getTitle());

        // Validate that null and duplicate invitees are silently skipped
        assertEquals(3, result.getParticipants().size());
 
        // Validate if the invite status of the organizer gets automatically ACCEPTED
        // and the other invitess have the status PENDING
        result.getParticipants().forEach(p -> {
            if (p.getUser().getUsername().equals("nuno")) {
                assertEquals(InviteStatus.ACCEPTED, p.getStatus());
            } else {
                assertEquals(InviteStatus.PENDING, p.getStatus());
            }
        });
 
        verify(meetingRepository, times(1)).save(any(Meeting.class));
        verify(userRepository, times(1)).findByUsername("invitee1");
        verify(userRepository, times(1)).findByUsername("invitee2");
        
        verify(userRepository, never()).findByUsername("nuno");
    }

    /**
     * Test when invitees list is empty
     */
    @Test
    void propose_InviteesListIsEmpty() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        List<String> emptyInvitees = List.of();

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));

        Meeting result = meetingService.propose(organizer, "Solo Meeting", "Desc", start, end, emptyInvitees);

        assertNotNull(result);
        // Validate that only the organizer is added as participant
        assertEquals(1, result.getParticipants().size());
        verify(meetingRepository).save(any(Meeting.class));

        // Validates that the userRepository is not consulted
        verifyNoInteractions(userRepository);
    }


    /**
     * Tests that calendarFor() calls "findCalendarMeetings()"
     * and returns the right result (unchanged, unfiltered, etc...)
     */
    @Test
    void calendarFor_ReturnRepositoryResult() {
        List<Meeting> expected = List.of(mock(Meeting.class));
        when(meetingRepository.findCalendarMeetings(organizer)).thenReturn(expected);
 
        List<Meeting> result = meetingService.calendarFor(organizer);
 
        assertSame(expected, result);

        // Verifies that the findCalendarMeetings() method is called
        verify(meetingRepository).findCalendarMeetings(organizer);
    }



    /**
     * Tests that pendingInvitesFo() calls "findByUserAndStatus()"
     * and returns the right result (unchanged, unfiltered, etc...)
     */
    @Test
    void pendingInvitesFor_ReturnRepositoryResult() {
        List<MeetingParticipant> expected = List.of(mock(MeetingParticipant.class));
        when(participantRepository.findByUserAndStatus(organizer, InviteStatus.PENDING))
                .thenReturn(expected);
 
        List<MeetingParticipant> result = meetingService.pendingInvitesFor(organizer);
 
        assertSame(expected, result);

        // Verifies that the findByUserAndStatus() method is called
        verify(participantRepository).findByUserAndStatus(organizer, InviteStatus.PENDING);
    }




    /**
     * Test that attempting to respond to an invite with a status different
     * from ACCEPTED or DECLINED throws an exception
     */
    @Test
    void respond_StatusIsNotAcceptedOrDeclined() {
        // Respond with a PENDING status
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> meetingService.respond(1L, organizer, InviteStatus.PENDING));
        assertEquals("Response must be ACCEPTED or DECLINED", ex.getMessage());
        verifyNoInteractions(participantRepository);
    }

    /**
     * Test if an IllegalArgumentException with meassge "No invite found for this use",
     * is thrown if there's no invite record for the user for that meeting
     */
    @Test
    void respond_NoInviteFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, organizer.getId()))
                .thenReturn(Optional.empty());
 
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> meetingService.respond(1L, organizer, InviteStatus.ACCEPTED));
        assertEquals("No invite found for this user", ex.getMessage());
    }

    /**
     * Test that the ACCEPT response sets the status correctly
     */
    @Test
    void respond_InviteFound() {
        MeetingParticipant participant = mock(MeetingParticipant.class);
        when(participantRepository.findByMeetingIdAndUserId(1L, organizer.getId()))
                .thenReturn(Optional.of(participant));
 
        meetingService.respond(1L, organizer, InviteStatus.ACCEPTED);
 
        verify(participant).setStatus(InviteStatus.ACCEPTED);
    }
 
    /**
     * Test that the DECLINE response sets the status correctly
     */
    @Test
    void respond_Declined() {
        MeetingParticipant participant = mock(MeetingParticipant.class);
        when(participantRepository.findByMeetingIdAndUserId(2L, invitee1.getId()))
                .thenReturn(Optional.of(participant));
 
        meetingService.respond(2L, invitee1, InviteStatus.DECLINED);
 
        verify(participant).setStatus(InviteStatus.DECLINED);
    }




    /**
     * Test that the event is correctly created and the description 
     * includes description, venue, source and url
     * Test undirectly the private "buildDescription()" method with existing values
     * for source, title, description, start, end, url and venue
     */
    @Test
    void copyFromDiscovered_EndTimePresent() {
        Instant start = Instant.now();
        Instant end   = start.plusSeconds(7200);
 
        DiscoveredEvent event = new DiscoveredEvent(
                "ticketline",
                "evt-001",
                "Drake Concert",
                "Drake world tour",
                start,
                end,
                "https://www.ticketline.pt/", 
                "Altice Arena"
        );
 
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));
 
        Meeting result = meetingService.copyFromDiscovered(organizer, event);
 
        // Validate the meeting basic fields
        assertNotNull(result);
        assertEquals("Drake Concert", result.getTitle());
        assertEquals(end, result.getEndTime());
        // Validate that the organizer was added as the only participant with ACCEPTED status
        assertEquals(1, result.getParticipants().size());
        assertEquals(InviteStatus.ACCEPTED,
                result.getParticipants().iterator().next().getStatus());
 
        // Validate that the description contains all expected fields
        String desc = result.getDescription();
        assertTrue(desc.contains("Drake world tour"));
        assertTrue(desc.contains("Venue: Altice Arena"));
        assertTrue(desc.contains("Source: ticketline"));
        assertTrue(desc.contains("https://www.ticketline.pt/"));
 
        verify(meetingRepository).save(any(Meeting.class));
    }

    /**
     * Tests if an event has no end time time, a default end time is applied
     * Test undirectly the private "buildDescription()" method with NULL description, NULL venue and NULL url
     */
    @Test
    void copyFromDiscovered_EndIsNull() {
        Instant start = Instant.now();
 
        DiscoveredEvent event = new DiscoveredEvent(
                "ticketmaster",
                "game-1",
                "Game",
                null,
                start,
                null,
                null,
                null
        );
 
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));
 
        Meeting result = meetingService.copyFromDiscovered(organizer, event);
 
        // Valitade the default applied ending time
        assertEquals(start.plusSeconds(7200), result.getEndTime());
 
        String desc = result.getDescription();
        assertFalse(desc.contains("Venue:"));
        assertFalse(desc.contains("("));
        assertTrue(desc.contains("Source: ticketmaster"));
 
        verify(meetingRepository).save(any(Meeting.class));
    }

    /**
     * Tests that blank description and venue fields are correctly filtered out 
     * from the meeting description
     */
    @Test
    void copyFromDiscovered_BlankDescriptionAndVenue() {
        Instant start = Instant.now();
        DiscoveredEvent event = new DiscoveredEvent(
                "ticketmaster",
                "id-2",
                "Concert Blank",
                "   ",
                start,
                null,
                "https://ticketmaster.com",
                "  "
        );

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));

        Meeting result = meetingService.copyFromDiscovered(organizer, event);

        String desc = result.getDescription();
        
        assertFalse(desc.contains("Venue:"));
        assertTrue(desc.contains("Source: ticketmaster"));
        assertTrue(desc.contains("(https://ticketmaster.com)"));
        
        assertFalse(desc.startsWith("   \n\n")); 
        verify(meetingRepository).save(any(Meeting.class));
    }



    /**
     * Test that an invalid iCal token throws an IllegalArgumentException, "Invalid iCal token"
     */
    @Test
    void calendarForIcalToken_TokenInvalid() {
        // Simulate that the token does not match any user
        when(userRepository.findByIcalToken("bad-token")).thenReturn(Optional.empty());
 
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> meetingService.calendarForIcalToken("bad-token"));
        assertEquals("Invalid iCal token", ex.getMessage());
    }

    /**
     * Tests that a valid iCal token returns the meetings
     */
    @Test
    void calendarForIcalToken_TokenValid() {
        Meeting m = mock(Meeting.class);
        List<Meeting> repoResult = List.of(m);
 
        // Simulate that the token matches the organizer
        when(userRepository.findByIcalToken("valid-token")).thenReturn(Optional.of(organizer));
        when(meetingRepository.findCalendarMeetings(organizer)).thenReturn(repoResult);
 
        List<Meeting> result = meetingService.calendarForIcalToken("valid-token");
 
        assertEquals(1, result.size());
        assertTrue(result.contains(m));
        // Validate that the result is a copy, not the same list reference
        assertNotSame(repoResult, result);
    }


    
}
