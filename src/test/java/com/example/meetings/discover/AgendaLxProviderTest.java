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
 * Integration tests for AgendaLxProvider
 * WireMock is used
 */
@Tag("integration")
public class AgendaLxProviderTest {

    private WireMockServer wireMock;
    private AgendaLxProvider provider;

    // Generates a future date to ensure events are not filtered out as past events
    private static final String FUTURE_DATE = LocalDate.now().plusMonths(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE);

    @BeforeEach
    void setUp() {
        // Start a WireMock server on a dynamic port
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
 
        // Point the RestClient to the WireMock server
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
 
        provider = new AgendaLxProvider(client);
    }
 
    @AfterEach
    void tearDown() {
        // Stop the WireMock server after each test
        wireMock.stop();
    }

    /**
     * Test that a API response is correctly mapped to a DiscoveredEvent
     */
    @Test
    void search_ResponseIsValid() {
        // Simulate a valid API response with all fields populated
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        [{
                          "id": 1,
                          "title": { "rendered": "Concerto de Fado" },
                          "description": ["<p>Concerto na casa do fado</p>"],
                          "occurences": ["%s"],
                          "string_times": "sex: 21h30",
                          "link": "https://agendalx.pt/fado",
                          "venue": { "1": { "name": "Casa do Fado" } }
                        }]
                        """.formatted(FUTURE_DATE))));
 
        List<DiscoveredEvent> result = provider.search("fado");
 
        // Validate that all fields are correctly mapped to the DiscoveredEvent
        assertEquals(1, result.size());
        DiscoveredEvent event = result.get(0);
        assertEquals("Concerto de Fado", event.title());
        assertEquals("1", event.externalId());
        assertEquals("https://agendalx.pt/fado", event.url());
        assertEquals("Casa do Fado", event.venue());
        assertEquals("Agenda Cultural de Lisboa", event.source());
        assertTrue(event.description().contains("Concerto na casa do fado"));
        assertNotNull(event.start());
    }

    /**
     * Tests that HTML tags in the description field are stripped from the mapped event
     */
    @Test
    void search_StripHtmlTagsFromDescription() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        [{
                          "id": 2,
                          "title": { "rendered": "Concerto de Fado" },
                          "description": ["<p>Concerto na <b>casa do fado</b></p>"],
                          "occurences": ["%s"],
                          "string_times": "19h00"
                        }]
                        """.formatted(FUTURE_DATE))));
 
        List<DiscoveredEvent> result = provider.search("art");
 
        assertEquals(1, result.size());
        String desc = result.get(0).description();

        // Validate that HTML tags were reomoved from the description
        assertFalse(desc.contains("<p>"));
        assertTrue(desc.contains("Concerto"));
        assertTrue(desc.contains("casa"));
    }

    /**
     * Tests that when string_times cannot be parsed, the fallback time of 20:00 is applied
     */
    @Test
    void search_FallBackTime() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        [{
                          "id": 3,
                          "title": { "rendered": "No Time Event" },
                          "occurences": ["%s"],
                          "string_times": "horário a confirmar"
                        }]
                        """.formatted(FUTURE_DATE))));
 
        List<DiscoveredEvent> result = provider.search("event");
 
        assertEquals(1, result.size());
        // Validate that a fallback time was applied
        assertNotNull(result.get(0).start());
    }

    /**
     * Test that past events are excluded from the results
     */
    @Test
    void search_PastEvents() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        [{
                          "id": 4,
                          "title": { "rendered": "Past Event" },
                          "occurences": ["2000-01-01"],
                          "string_times": "21h00"
                        }]
                        """)));
 
        List<DiscoveredEvent> result = provider.search("event");
 
        assertTrue(result.isEmpty());
    }

    /**
     * Tests that events with a blank title are excluded from results
     */
    @Test
    void search_TitleIsBlank() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        [{
                          "id": 5,
                          "title": { "rendered": "   " },
                          "occurences": ["%s"],
                          "string_times": "21h00"
                        }]
                        """.formatted(FUTURE_DATE))));
 
        List<DiscoveredEvent> result = provider.search("event");
 
        assertTrue(result.isEmpty());
    }

    /**
     * Tests that the provider returns an empty list when the API responds with an error status
     */
    @Test
    void search_ApiReturnsError() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(serverError()));
 
        List<DiscoveredEvent> result = provider.search("concerto");
 
        assertTrue(result.isEmpty());
    }
    
}
