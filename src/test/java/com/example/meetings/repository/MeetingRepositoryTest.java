package com.example.meetings.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;

/**
 * Database integration tests for MeetingRepository
 */
@Tag("integration")
@DataJpaTest
public class MeetingRepositoryTest {

    @Autowired private MeetingRepository meetingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingParticipantRepository participantRepository;

    private User organizer;
    private User invitee;

    @BeforeEach
    void setUp() {
        organizer = userRepository.save(new User("nuno", "nuno@gmail.pt", "hashed"));
        invitee   = userRepository.save(new User("invitee1", "invitee1@gmail.pt", "hashed"));
    }

    /**
     * Test if findCalendarMeetings() returns meetings where the user is the organizer
     */
    @Test
    void findCalendarMeetings_UserIsOrganizer() {
        Meeting meeting = meetingRepository.save(new Meeting("Meeting 1", null,
                Instant.parse("2027-06-15T09:00:00Z"),
                Instant.parse("2027-06-15T09:30:00Z"), organizer));
 
        List<Meeting> result = meetingRepository.findCalendarMeetings(organizer);
 
        assertEquals(1, result.size());
        assertEquals("Meeting 1", result.get(0).getTitle());
    }

    /**
     * Test if findCalendarMeetings() returns meetings where the user is an ACCEPTED participant
     */
    @Test
    void findCalendarMeetings_UserIsAcceptedParticipant() {
        Meeting meeting = meetingRepository.save(new Meeting("Meeting 2", null,
                Instant.parse("2027-06-15T10:00:00Z"),
                Instant.parse("2027-06-15T11:00:00Z"), organizer));
        participantRepository.save(new MeetingParticipant(meeting, invitee, InviteStatus.ACCEPTED));
 
        List<Meeting> result = meetingRepository.findCalendarMeetings(invitee);
 
        assertEquals(1, result.size());
        assertEquals("Meeting 2", result.get(0).getTitle());
    }

    /**
     * Test if findCalendarMeetings() returns meetings where the user is a PENDING participant
     */
    @Test
    void findCalendarMeetings_UserIsPendingParticipant() {
        Meeting meeting = meetingRepository.save(new Meeting("Meeting 3", null,
                Instant.parse("2027-06-15T14:00:00Z"),
                Instant.parse("2027-06-15T15:00:00Z"), organizer));
        participantRepository.save(new MeetingParticipant(meeting, invitee, InviteStatus.PENDING));
 
        List<Meeting> result = meetingRepository.findCalendarMeetings(invitee);
 
        assertEquals(1, result.size());
        assertEquals("Meeting 3", result.get(0).getTitle());
    }

    /**
     * Test if findCalendarMeetings() excludes all meeting that the user has DECLINED from the results
     */
    @Test
    void findCalendarMeetings_UserDeclined() {
        Meeting meeting = meetingRepository.save(new Meeting("Meeting 4", null,
                Instant.parse("2027-06-15T16:00:00Z"),
                Instant.parse("2027-06-15T17:00:00Z"), organizer));
        participantRepository.save(new MeetingParticipant(meeting, invitee, InviteStatus.DECLINED));
 
        List<Meeting> result = meetingRepository.findCalendarMeetings(invitee);
 
        assertTrue(result.isEmpty());
    }

    /**
     * Test if findCalendarMeetings() returns results sorted by startTime ascending
     */
    @Test
    void findCalendarMeetings_SortedByStartTime() {
        meetingRepository.save(new Meeting("Later", null,
                Instant.parse("2027-06-15T11:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"), organizer));
        meetingRepository.save(new Meeting("Earlier", null,
                Instant.parse("2027-06-15T09:00:00Z"),
                Instant.parse("2027-06-15T10:00:00Z"), organizer));
 
        List<Meeting> result = meetingRepository.findCalendarMeetings(organizer);
 
        assertEquals(2, result.size());
        assertEquals("Earlier", result.get(0).getTitle());
        assertEquals("Later", result.get(1).getTitle());
    }

    /**
     * Test if findOverlapping() returns a meeting that overlaps the given time window
     */
    @Test
    void findOverlapping_MeetingOverlaps() {
        meetingRepository.save(new Meeting("Overlap", null,
                Instant.parse("2027-06-15T09:00:00Z"),
                Instant.parse("2027-06-15T10:00:00Z"), organizer));
 
        List<Meeting> result = meetingRepository.findOverlapping(organizer,
                Instant.parse("2027-06-15T09:30:00Z"),
                Instant.parse("2027-06-15T11:00:00Z"));
 
        assertEquals(1, result.size());
        assertEquals("Overlap", result.get(0).getTitle());
    }

    /**
     * Test if findOverlapping() returns empty when no meeting overlaps the given time window
     */
    @Test
    void findOverlapping_NoMeetingOverlaps() {
        meetingRepository.save(new Meeting("No Overlap", null,
                Instant.parse("2027-06-15T09:00:00Z"),
                Instant.parse("2027-06-15T10:00:00Z"), organizer));
 
        List<Meeting> result = meetingRepository.findOverlapping(organizer,
                Instant.parse("2027-06-15T11:00:00Z"),
                Instant.parse("2027-06-15T12:00:00Z"));
 
        assertTrue(result.isEmpty());
    }

    
}
