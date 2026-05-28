package com.example.meetings.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
 * Database integration tests for MeetingParticipantRepository
 */
@Tag("integration")
@DataJpaTest
public class MeetingParticipantRepositoryTest {

    @Autowired private MeetingParticipantRepository participantRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private UserRepository userRepository;

    private User organizer;
    private User invitee;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        organizer = userRepository.save(new User("nuno", "nuno@gmail.pt", "hashed"));
        invitee   = userRepository.save(new User("invitee1", "invitee1@gmail.pt", "hashed"));
        meeting   = meetingRepository.save(new Meeting("Meeting 1", null,
                Instant.parse("2027-06-15T09:00:00Z"),
                Instant.parse("2027-06-15T09:30:00Z"), organizer));
    }


    /**
     * Test if findByUserAndStatus() returns PENDING invites for a user
     */
    @Test
    void findByUserAndStatus_PendingInvites() {
        participantRepository.save(new MeetingParticipant(meeting, invitee, InviteStatus.PENDING));
 
        List<MeetingParticipant> result = participantRepository
                .findByUserAndStatus(invitee, InviteStatus.PENDING);
 
        assertEquals(1, result.size());
        assertEquals(InviteStatus.PENDING, result.get(0).getStatus());
        assertEquals("invitee1", result.get(0).getUser().getUsername());
    }

    /**
     * Test if findByUserAndStatus() returns empty when there's no correspondence (invites with that status)
     */
    @Test
    void findByUserAndStatus_NoMatchingStatus() {
        participantRepository.save(new MeetingParticipant(meeting, invitee, InviteStatus.ACCEPTED));
 
        List<MeetingParticipant> result = participantRepository
                .findByUserAndStatus(invitee, InviteStatus.PENDING);
 
        assertTrue(result.isEmpty());
    }

    /**
     * Test if findByUserAndStatus() only returns invites for the specified user and status
     */
    @Test
    void findByUserAndStatus_InvitesForTheSpecifiedUser() {
        User otherUser = userRepository.save(new User("other", "other@gmail.pt", "hashed"));
        participantRepository.save(new MeetingParticipant(meeting, invitee, InviteStatus.PENDING));
        participantRepository.save(new MeetingParticipant(meeting, otherUser, InviteStatus.PENDING));
 
        List<MeetingParticipant> result = participantRepository
                .findByUserAndStatus(invitee, InviteStatus.PENDING);
 
        assertEquals(1, result.size());
        assertEquals("invitee1", result.get(0).getUser().getUsername());
    }

    /**
     * Test if findByMeetingIdAndUserId() returns the participant when the meeting and invited user match
     */
    @Test
    void findByMeetingIdAndUserId_WhenExists() {
        participantRepository.save(new MeetingParticipant(meeting, invitee, InviteStatus.PENDING));
 
        Optional<MeetingParticipant> result = participantRepository
                .findByMeetingIdAndUserId(meeting.getId(), invitee.getId());
 
        assertTrue(result.isPresent());
        assertEquals(InviteStatus.PENDING, result.get().getStatus());
    }

    /**
     * Test if findByMeetingIdAndUserId() returns empty when no participant for that meeting matches
     */
    @Test
    void findByMeetingIdAndUserId_NotFound() {
        Optional<MeetingParticipant> result = participantRepository
                .findByMeetingIdAndUserId(meeting.getId(), invitee.getId());
 
        assertTrue(result.isEmpty());
    }

    
    
}
