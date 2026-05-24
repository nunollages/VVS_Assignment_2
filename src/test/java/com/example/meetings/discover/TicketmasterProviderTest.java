package com.example.meetings.discover;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
 
import java.util.List;
 
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TicketmasterProvider
 * WireMock is used
 */
@Tag("integration")
public class TicketmasterProviderTest {
    
    private WireMockServer wireMock;
    private TicketmasterProvider provider;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
 
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
 
        provider = new TicketmasterProvider("test-api-key", "PT", client);
    }
 
    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    /**
     * Test that a API response is correctly mapped to a DiscoveredEvent
     */
    @Test
    void search_ResponseIsValid() {
        wireMock.stubFor(get(urlPathMatching("/events.json"))
                .willReturn(okJson("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "tm-001",
                              "name": "Rock Concert",
                              "url": "https://ticketmaster.com/rock",
                              "info": "Great show",
                              "dates": { "start": { "dateTime": "2027-06-15T20:00:00Z" } },
                              "_embedded": { "venues": [{ "name": "Altice Arena" }] }
                            }]
                          }
                        }
                        """)));
 
        List<DiscoveredEvent> result = provider.search("rock");
 
        assertEquals(1, result.size());
        DiscoveredEvent event = result.get(0);
        assertEquals("Rock Concert", event.title());
        assertEquals("tm-001", event.externalId());
        assertEquals("https://ticketmaster.com/rock", event.url());
        assertEquals("Great show", event.description());
        assertEquals("Altice Arena", event.venue());
        assertEquals("Ticketmaster", event.source());
        assertNotNull(event.start());
    }

    /**
     * Tests that events with no date are skipped
     */
    @Test
    void search_DateTimeIsMissing() {
        wireMock.stubFor(get(urlPathMatching("/events.json"))
                .willReturn(okJson("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "tm-002",
                              "name": "TBA Event",
                              "dates": { "start": {} }
                            }]
                          }
                        }
                        """)));
 
        List<DiscoveredEvent> result = provider.search("tba");
 
        assertTrue(result.isEmpty());
    }

     /**
     * Tests that a response with no _embedded field produces an empty list
     */
    @Test
    void search_ResponseHasNoEmbedded() {
        wireMock.stubFor(get(urlPathMatching("/events.json"))
                .willReturn(okJson("{}")));
 
        List<DiscoveredEvent> result = provider.search("concert");
 
        assertTrue(result.isEmpty());
    }

    /**
     * Tests that events with no venue will have a null venue field
     */
    @Test
    void search_NoVenueInResponse() {
        wireMock.stubFor(get(urlPathMatching("/events.json"))
                .willReturn(okJson("""
                        {
                          "_embedded": {
                            "events": [{
                              "id": "tm-003",
                              "name": "No Venue Event",
                              "dates": { "start": { "dateTime": "2027-06-15T20:00:00Z" } }
                            }]
                          }
                        }
                        """)));
 
        List<DiscoveredEvent> result = provider.search("event");
 
        assertEquals(1, result.size());
        assertNull(result.get(0).venue());
    }

    /**
     * Tests that the provider returns an empty list when the API responds with an error status
     */
    @Test
    void search_ApiReturnsError() {
        wireMock.stubFor(get(urlPathMatching("/events.json"))
                .willReturn(serverError()));
 
        List<DiscoveredEvent> result = provider.search("concert");
 
        assertTrue(result.isEmpty());
    }
}

