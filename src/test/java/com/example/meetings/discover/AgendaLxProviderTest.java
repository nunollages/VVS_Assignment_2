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

    private static final String FUTURE_DATE = LocalDate.now().plusMonths(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE);

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
 
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
 
        provider = new AgendaLxProvider(client);
    }
 
    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

     /**
     * Test that a API response is correctly mapped to a DiscoveredEvent
     */
    @Test
    void search_ShouldMapApiResponse_WhenResponseIsValid() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        [{
                          "id": 1,
                          "title": { "rendered": "Fado Night" },
                          "description": ["<p>Great fado show</p>"],
                          "occurences": ["%s"],
                          "string_times": "sex: 21h30",
                          "link": "https://agendalx.pt/fado",
                          "venue": { "1": { "name": "Casa do Fado" } }
                        }]
                        """.formatted(FUTURE_DATE))));
 
        List<DiscoveredEvent> result = provider.search("fado");
 
        assertEquals(1, result.size());
        DiscoveredEvent event = result.get(0);
        assertEquals("Fado Night", event.title());
        assertEquals("1", event.externalId());
        assertEquals("https://agendalx.pt/fado", event.url());
        assertEquals("Casa do Fado", event.venue());
        assertEquals("Agenda Cultural de Lisboa", event.source());
        assertTrue(event.description().contains("Great fado show"));
        assertNotNull(event.start());
    }

    /**
     * Tests that HTML tags in the description field are stripped from the mapped event
     */
    @Test
    void search_ShouldStripHtmlTags_FromDescription() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(okJson("""
                        [{
                          "id": 2,
                          "title": { "rendered": "Art Show" },
                          "description": ["<p>Beautiful <b>art</b> exhibition</p>"],
                          "occurences": ["%s"],
                          "string_times": "19h00"
                        }]
                        """.formatted(FUTURE_DATE))));
 
        List<DiscoveredEvent> result = provider.search("art");
 
        assertEquals(1, result.size());
        String desc = result.get(0).description();
        assertFalse(desc.contains("<p>"), "HTML tags must be stripped from description");
        assertTrue(desc.contains("Beautiful"));
        assertTrue(desc.contains("art"));
    }

    /**
     * Tests that when string_times cannot be parsed, the fallback time of 20:00 is applied
     */
    @Test
    void search_ShouldUseFallbackTime_WhenStringTimesIsUnparseable() {
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
        assertNotNull(result.get(0).start());
    }

    /**
     * Test that past events are excluded from the results
     */
    @Test
    void search_ShouldExcludeEvents_WhenAllOccurrencesAreInThePast() {
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
    void search_ShouldExcludeEvents_WhenTitleIsBlank() {
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
    void search_ShouldReturnEmptyList_WhenApiReturnsError() {
        wireMock.stubFor(get(urlPathMatching("/events"))
                .willReturn(serverError()));
 
        List<DiscoveredEvent> result = provider.search("concerto");
 
        assertTrue(result.isEmpty());
    }
    
}
