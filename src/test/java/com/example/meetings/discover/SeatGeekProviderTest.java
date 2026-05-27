package com.example.meetings.discover;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
 
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
 
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SeatGeekProvider
 * WireMock is used
 */
@Tag("integration")
public class SeatGeekProviderTest {

    private WireMockServer wireMock;
    private SeatGeekProvider provider;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
 
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
 
        provider = new SeatGeekProvider("test-client-id", client);
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
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        {
                          "events": [{
                            "id": 123,
                            "title": "Jazz Night",
                            "short_title": "Jazz",
                            "datetime_utc": "2027-08-20T21:00:00",
                            "url": "https://seatgeek.com/jazz",
                            "description": "A smooth night",
                            "venue": { "name": "Hot Clube" }
                          }]
                        }
                        """)));
 
        List<DiscoveredEvent> result = provider.search("jazz");
 
        assertEquals(1, result.size());
        DiscoveredEvent event = result.get(0);
        assertEquals("Jazz Night", event.title());
        assertEquals("123", event.externalId());
        assertEquals("https://seatgeek.com/jazz", event.url());
        assertEquals("A smooth night", event.description());
        assertEquals("Hot Clube", event.venue());
        assertEquals("SeatGeek", event.source());
        assertNotNull(event.start());
    }

    /**
     * Tests that when title is null in the API response, short_title is used as fallback
     */
    @Test
    void search_TitleIsNull() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        {
                          "events": [{
                            "id": 456,
                            "short_title": "Short Title",
                            "datetime_utc": "2027-08-20T21:00:00"
                          }]
                        }
                        """)));
 
        List<DiscoveredEvent> result = provider.search("event");
 
        assertEquals(1, result.size());
        assertEquals("Short Title", result.get(0).title());
    }


    /**
     * Tests that events with a wrong datetime format are skipped
     */
    @Test
    void search_WrongDatetimeFormat() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        {
                          "events": [{
                            "id": 789,
                            "title": "Bad Date Event",
                            "datetime_utc": "not-a-date"
                          }]
                        }
                        """)));
 
        List<DiscoveredEvent> result = provider.search("event");
 
        assertTrue(result.isEmpty());
    }

    /**
     * Tests that a response with an empty events array produces an empty list
     */
    @Test
    void search_ResponseHasNoEvents() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("{\"events\": []}")));
 
        List<DiscoveredEvent> result = provider.search("concert");
 
        assertTrue(result.isEmpty());
    }

    /**
     * Tests that the provider returns an empty list when the API responds with an error status
     */
    @Test
    void search_ApiReturnsError() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(serverError()));
 
        List<DiscoveredEvent> result = provider.search("concert");
 
        assertTrue(result.isEmpty());
    }

    
}
