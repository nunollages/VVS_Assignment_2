package com.example.meetings.unitTestsBusinessLogic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.service.ICalService;

/**
 * Unit tests for ICalService class
 * This class validates the logic for converting internal domain models (Meeting, User) 
 * into the standard iCalendar (RFC 5545) text format
 * 
 * There was no need to use Mockito because the target class has no dependencies
 *
 * Criteria: Line and Branch Coverage
 * Goal: 100%
 */
public class ICalServiceTest {

    private ICalService iCalService;
 
    private User organizer;
 
    @BeforeEach
    void setUp() {
        iCalService = new ICalService();
        // Before each test, a user is created
        organizer = new User("nuno", "nuno@gmail.pt", "hash-123");
    }

    /**
     * Test that the function rerturns a valid VCALENDAR with only
     * header and footer and no VEVENT blocks (valid output), even if no events exist
     */
    @Test
    void render_MeetingListIsEmpty() {
        // No meetings were passed
        String result = iCalService.render(organizer, List.of());
 
        // Validates that all the fields are included
        assertTrue(result.contains("BEGIN:VCALENDAR"));
        assertTrue(result.contains("VERSION:2.0"));
        assertTrue(result.contains("PRODID:-//meetings-app//EN"));
        assertTrue(result.contains("X-WR-CALNAME:nuno's meetings"));
        assertTrue(result.contains("END:VCALENDAR"));
        assertFalse(result.contains("BEGIN:VEVENT"));
        assertTrue(result.contains("\r\n"));
    }

    /**
     * Test that the description is included when the meeting has one
     */
    @Test
    void render_MeetingHasDescription() {
        Meeting meeting = buildMeeting("Meetign 1", "The description", InviteStatus.ACCEPTED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("DESCRIPTION:The description"));
    }

    /**
     * Test that the description is omited when the meeting description is null
     */
    @Test
    void render_NullDescription() {
        Meeting meeting = buildMeeting("Meeting 2", null, InviteStatus.ACCEPTED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertFalse(result.contains("DESCRIPTION:"));
    }

    /**
     * Test that the description is omited when the meeting description is blank
     */
    @Test
    void render_DescriptionIsBlank() {
        Meeting meeting = buildMeeting("Meeting 2", "  ", InviteStatus.ACCEPTED);
        
        String result = iCalService.render(organizer, List.of(meeting));

        assertFalse(result.contains("DESCRIPTION:")); 
        assertTrue(result.contains("SUMMARY:Meeting 2"));
    }

    /**
     * Test that STATUS:CONFIRMED when all participants accept the invites
     */
    @Test
    void render_ConfirmedStatus() {
        Meeting meeting = buildMeeting("Meeting 3", null, InviteStatus.ACCEPTED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("STATUS:CONFIRMED"));
    }

    /**
     * Test that STATUS:TENTATIVE when at least one participant has not accepted the invite
     */
    @Test
    void render_TentativeStatus() {
        Meeting meeting = buildMeeting("Meeting 4", null, InviteStatus.PENDING);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("STATUS:TENTATIVE"));
    }

    /**
     * Tests that an ACCEPTED participant produces PARTSTAT=ACCEPTED
     */
    @Test
    void render_AcceptedPartStat() {
        Meeting meeting = buildMeeting("Meeting 5", null, InviteStatus.ACCEPTED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("PARTSTAT=ACCEPTED"));
    }

    /**
     * Tests that a DECLINED participant produces PARTSTAT=DECLINED
     */
    @Test
    void render_DeclinedPartStat() {
        Meeting meeting = buildMeeting("Meeting 6", null, InviteStatus.DECLINED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("PARTSTAT=DECLINED"));
    }

    /**
     * Tests that a PENDING participant produces PARTSTAT=NEEDS-ACTION
     */
    @Test
    void render_NeedsActionPartStat() {
        Meeting meeting = buildMeeting("Meeting 7", null, InviteStatus.PENDING);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("PARTSTAT=NEEDS-ACTION"));
    }
    /**
     * Tests indirectly the escape() method using special characters in the title of the meeting
     */
    @Test
    void render_EscapeSpecialCharacters() {
        // Title contains all special characters that escape() must handle
        Meeting meeting = buildMeeting("Meet\\ing;One,Two\nThree\rFour", null, InviteStatus.ACCEPTED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("SUMMARY:Meet\\\\ing\\;One\\,Two\\nThreeFour"));
    }

    /**
     * Tests that the escape() method handles null values in user fields correctly
     * (in this case email = null) and does not throw NullPointerExceptions
     */
    @Test
    void render_NullValue_InEscape() {
        User userWithSpecialChars = new User("nuno", null, "hashed") {
            @Override public String getEmail() { return null; }
        };
        Meeting meeting = buildMeetingWithOrganizer("Meeting 8", null, InviteStatus.ACCEPTED, userWithSpecialChars);
 
        assertDoesNotThrow(() -> iCalService.render(userWithSpecialChars, List.of(meeting)));
    }


    // HELPERS

    /**
     * Builds a Meeting with a single participant using the class organizer
     */
    private Meeting buildMeeting(String title, String description, InviteStatus participantStatus) {
        return buildMeetingWithOrganizer(title, description, participantStatus, organizer);
    }

    /**
     * Builds a Meeting with a single participant + the organizer
     */
    private Meeting buildMeetingWithOrganizer(String title, String description,
                                               InviteStatus participantStatus, User meetingOrganizer) {
        Instant start = Instant.now();
        Instant end   = start.plusSeconds(3600);
        Meeting meeting = new Meeting(title, description, start, end, meetingOrganizer);
 
        User participant = new User("invitee1", "invitee1@gmail.pt", "hashed");
        meeting.addParticipant(new MeetingParticipant(meeting, participant, participantStatus));
 
        return meeting;
    }

    
}
