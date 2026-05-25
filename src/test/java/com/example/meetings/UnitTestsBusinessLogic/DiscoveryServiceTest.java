package com.example.meetings.UnitTestsBusinessLogic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.DiscoveryService;
import com.example.meetings.discover.EventProvider;

import java.time.Instant;
import java.util.List;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DiscoveryService class
 * This class validates the business logic for event discovery and aggregation
 * 
 * Criteria: Line and Branch Coverage
 * Goal: 100%
 */
@ExtendWith(MockitoExtension.class)
public class DiscoveryServiceTest {

    @Mock
    private EventProvider providerA;
 
    @Mock
    private EventProvider providerB;
 
    private DiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new DiscoveryService(List.of(providerA, providerB));
    }

    /**
     * Test the providers() method
     */
    @Test
    void providers_Test() {
        List<EventProvider> result = discoveryService.providers();
        assertEquals(2, result.size());
        assertTrue(result.contains(providerA));
        assertTrue(result.contains(providerB));
    }

    /**
     * Test that the function should return an empty list when the query is null
     */
    @Test
    void search_NullQuery() {
        List<DiscoveredEvent> result = discoveryService.search(null);
 
        assertTrue(result.isEmpty());
        // Validate that there where no unnecessary network requests to the EventProviders
        verifyNoInteractions(providerA, providerB);
    }

    /**
     * Test that the function should return an empty list when the query is blank
     */
    @Test
    void search_BlankQuery() {
        List<DiscoveredEvent> result = discoveryService.search("   ");
 
        assertTrue(result.isEmpty());
        verifyNoInteractions(providerA, providerB);
    }

    /**
     * Test that providers that are not configured are skipped
     */
    @Test
    void search_UnconfiguredProviders() {
        when(providerA.isConfigured()).thenReturn(false);
        when(providerB.isConfigured()).thenReturn(false);
 
        List<DiscoveredEvent> result = discoveryService.search("Something");
 
        assertTrue(result.isEmpty());
        // Validate that no search operations are performed when providers are not configured
        verify(providerA, never()).search(anyString());
        verify(providerB, never()).search(anyString());
    }

    /**
     * Test empty results list
     */
    @Test
    void search_ProviderReturnsEmptyList() {
        when(providerA.isConfigured()).thenReturn(true);
        when(providerA.search("empty")).thenReturn(List.of());
        when(providerB.isConfigured()).thenReturn(false);

        List<DiscoveredEvent> result = discoveryService.search("empty");

        assertTrue(result.isEmpty());
        verify(providerA).search("empty");
    }

    /**
     * Test that the method returns the results from a configured provider
     */
    @Test
    void search_ConfiguredProvider() {
        Instant start = Instant.now();
        DiscoveredEvent event = new DiscoveredEvent(
                "ticketmaster", "evt-1", "Event", null,
                start, null, "https://www.ticketline.pt/", null);
 
        when(providerA.isConfigured()).thenReturn(true);
        when(providerA.search("event")).thenReturn(List.of(event));
        when(providerB.isConfigured()).thenReturn(false);
 
        List<DiscoveredEvent> result = discoveryService.search("event");
 
        assertEquals(1, result.size());
        assertSame(event, result.get(0));
    }

    /**
     * Test that when two events with the same URL from different providers
     * exist, only the first one is returned (deduplication)
     */
    @Test
    void search_DuplicatedUrls() {
        Instant start = Instant.now();
        DiscoveredEvent eventA = new DiscoveredEvent(
                "ticketmaster", "evt-1", "Concert", null,
                start, null, "https://www.ticketline.pt/", null);
        DiscoveredEvent eventB = new DiscoveredEvent(
                "seatgeek", "evt-2", "Concert", null,
                start, null, "https://www.ticketline.pt/", null);
 
        when(providerA.isConfigured()).thenReturn(true);
        when(providerA.search("concert")).thenReturn(List.of(eventA));
        when(providerB.isConfigured()).thenReturn(true);
        when(providerB.search("concert")).thenReturn(List.of(eventB));
 
        List<DiscoveredEvent> result = discoveryService.search("concert");
 
        assertEquals(1, result.size());
        assertSame(eventA, result.get(0));
    }

    /**
     * Test that when two events don't have url, but have the same source and externalId
     * (Have the same key ->  Key = e.source() + ":" + e.externalId()), it produces a single result
     */
    @Test
    void search_UrlNull_SameKey() {
        Instant start = Instant.now();
        DiscoveredEvent eventA = new DiscoveredEvent(
                "ticketmaster", "evt-1", "Concert", null,
                start, null, null, null);
        DiscoveredEvent eventB = new DiscoveredEvent(
                "ticketmaster", "evt-1", "Concert", null,
                start, null, null, null); 
 
        when(providerA.isConfigured()).thenReturn(true);
        when(providerA.search("concert")).thenReturn(List.of(eventA));
        when(providerB.isConfigured()).thenReturn(true);
        when(providerB.search("concert")).thenReturn(List.of(eventB));
 
        List<DiscoveredEvent> result = discoveryService.search("concert");
 
        assertEquals(1, result.size());
        assertSame(eventA, result.get(0));
    }

    /**
     * Test that the returned events are correctly sorted by time
     */
    @Test
    void search_SortResultsByStartTime() {
        Instant now = Instant.now();
        DiscoveredEvent later = new DiscoveredEvent(
                "ticketmaster", "evt-2", "Later Event", null,
                now.plusSeconds(7200), null, "https://www.ticketline.pt/2", null);
        DiscoveredEvent earlier = new DiscoveredEvent(
                "ticketmaster", "evt-1", "Earlier Event", null,
                now.plusSeconds(3600), null, "https://www.ticketline.pt/1", null);
 
        when(providerA.isConfigured()).thenReturn(true);
        when(providerA.search("concert")).thenReturn(List.of(later, earlier));
        when(providerB.isConfigured()).thenReturn(false);
 
        List<DiscoveredEvent> result = discoveryService.search("concert");
 
        assertEquals(2, result.size());
        assertSame(earlier, result.get(0));
        assertSame(later, result.get(1));
    }
    
}
