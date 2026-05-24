package com.example.meetings.UnitTestsBusinessLogic;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.service.ICalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
 
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test ICalService class
 * Criteria: Line and Branch Coverage
 * Goal: 100%
 */
public class ICalServiceTest {

    private ICalService iCalService;
 
    private User organizer;
 
    @BeforeEach
    void setUp() {
        iCalService = new ICalService();
        organizer = new User("nuno", "nuno@gmail.pt", "hash-123");
    }

    /**
     * Test that the function rerturns a valid VCALENDAR with only
     * header and footer and no VEVENT blocks
     */
    @Test
    void render_MeetingListIsEmpty() {
        String result = iCalService.render(organizer, List.of());
 
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

    @Test
    void render_DescriptionIsBlank() {
        User organizer = new User("alice", "alice@email.com", "123");
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        
        Meeting meetingWithBlankDesc = new Meeting("Blank Desc Meeting", "   ", start, end, organizer);
        
        String result = iCalService.render(organizer, List.of(meetingWithBlankDesc));

        assertFalse(result.contains("DESCRIPTION:")); 
        assertTrue(result.contains("SUMMARY:Blank Desc Meeting"));
    }

    /**
     * Test that STATUS:CONFIRMED when all participants accept
     */
    @Test
    void render_ConfirmedStatus_WhenAllParticipantsAccepted() {
        Meeting meeting = buildMeeting("Meeting 3", null, InviteStatus.ACCEPTED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("STATUS:CONFIRMED"));
    }

    /**
     * Test that STATUS:TENTATIVE when at least one participant has not accepted
     */
    @Test
    void render_ShouldProduceTentativeStatus_WhenParticipantIsPending() {
        Meeting meeting = buildMeeting("Meeting 4", null, InviteStatus.PENDING);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("STATUS:TENTATIVE"));
    }

    /**
     * Tests that an ACCEPTED participant produces PARTSTAT=ACCEPTED
     */
    @Test
    void render_AcceptedPartStat_WhenParticipantAccepted() {
        Meeting meeting = buildMeeting("Meeting 5", null, InviteStatus.ACCEPTED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("PARTSTAT=ACCEPTED"));
    }

    /**
     * Tests that a DECLINED participant produces PARTSTAT=DECLINED
     */
    @Test
    void render_DeclinedPartStat_WhenParticipantDeclined() {
        Meeting meeting = buildMeeting("Meeting 6", null, InviteStatus.DECLINED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("PARTSTAT=DECLINED"));
    }

    /**
     * Tests that a PENDING participant produces PARTSTAT=NEEDS-ACTION
     */
    @Test
    void render_NeedsActionPartStat_WhenParticipantIsPending() {
        Meeting meeting = buildMeeting("Meeting 7", null, InviteStatus.PENDING);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("PARTSTAT=NEEDS-ACTION"));
    }
    /**
     * Tests indirectly the escape() method using special characters in the title
     */
    @Test
    void render_ShouldEscapeSpecialCharacters_InMeetingTitle() {
        // Title contains all special characters that escape() must handle
        Meeting meeting = buildMeeting("Meet\\ing;One,Two\nThree\rFour", null, InviteStatus.ACCEPTED);
 
        String result = iCalService.render(organizer, List.of(meeting));
 
        assertTrue(result.contains("SUMMARY:Meet\\\\ing\\;One\\,Two\\nThreeFour"));
    }

    /**
     * Tests that a null organizer username is handled by escape() returning an empty string
     */
    @Test
    void render_ShouldHandleNullValue_InEscape() {
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
