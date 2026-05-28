package com.example.meetings.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.example.meetings.config.SecurityConfig;
import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.DiscoveryService;
import com.example.meetings.discover.EventProvider;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;

/**
 * End-to-End tests using Selenium WebDriver and a dedicated test database
 * Chrome runs in headless mode
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:e2edb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.base-url=http://localhost"
})
@Tag("e2e")
public class E2ETest {

    @LocalServerPort
    private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private MeetingParticipantRepository participantRepository;
    @MockBean private DiscoveryService discoverService;
 
    private WebDriver driver;
    private WebDriverWait wait;
 
    private String baseUrl() {
        return "http://localhost:" + port;
    }
 
    @BeforeEach
    void setUp() {
        // Clear database manually — avoids restarting the Spring context
        // which would change the port and invalidate the browser session
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }
 
    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                driver.manage().deleteAllCookies();
            } catch (Exception ignored) {}
            driver.quit();
        }
    }

    // HELPERS

    private void register(String username, String email, String password) {
        driver.get(baseUrl() + "/register");
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        wait.until(ExpectedConditions.urlContains("/login"));

    }

    private void login(String username, String password) {
        driver.get(baseUrl() + "/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.clear();
        passwordField.clear();

        usernameField.sendKeys(username);
        passwordField.sendKeys(password);

        WebElement submit =
            driver.findElement(By.cssSelector("button[type=submit]"));

        wait.until(ExpectedConditions.elementToBeClickable(submit));

        submit.click();

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/calendar"),
            ExpectedConditions.urlContains("error")
        ));
    }

    private void registerAndLogin(String username, String email, String password) {
        register(username, email, password);
        wait.until(ExpectedConditions.urlContains("/login"));
        login(username, password);
        wait.until(ExpectedConditions.urlContains("/calendar"));
    }

    private void setDateTimeLocal(String id, String value) {
        WebElement el = driver.findElement(By.id(id));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].type = 'text';",
            el, value
        );
    }

    private void submitProposalForm() {
        driver.findElement(By.id("title")).submit();
    }



    /**
     * Test that a new user can register and after it's redirected to the login page
     * with a success message
     */
    @Test
    void register_Register_Success() {
        register("nuno", "nuno@gmail.pt", "password123");
 
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getPageSource().contains("Account created"));
    }

    /**
     * Test if registering with an already existing username shows an error message in the form
     */
    @Test
    void register_UsernameAlreadyExists() {
        register("nuno", "nuno@gmail.pt", "password123");
        
        // Submit again with the same username
        driver.get(baseUrl() + "/register");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).sendKeys("nuno");
        driver.findElement(By.id("email")).sendKeys("other@gmail.pt");
        driver.findElement(By.id("password")).sendKeys("password456");
        driver.findElement(By.cssSelector("button[type=submit]")).click();
 
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".error")));
        assertTrue(driver.getPageSource().contains("Username already taken"));
        assertTrue(driver.getCurrentUrl().contains("/register"));
    }

    /**
     * Test if the login redirects to the calendar page
     */
    @Test
    void login_RedirectsToCalendar() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");
 
        assertTrue(driver.getCurrentUrl().contains("/calendar"));
        assertTrue(driver.getPageSource().contains("Your calendar"));
    }

    /**
     * Test if shows an error when the credentials are wrong
     */
    @Test
    void login_WrongCredentials() {
        register("nuno", "nuno@gmail.pt", "password123");
 
        driver.get(baseUrl() + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).sendKeys("nuno");
        driver.findElement(By.id("password")).sendKeys("wrongpassword");
        driver.findElement(By.cssSelector("button[type=submit]")).click();
 
        wait.until(ExpectedConditions.urlContains("error"));
        assertTrue(driver.getPageSource().contains("Invalid username or password"));
    }

    /**
     * Test if accessing /calendar without being logged in redirects to the login page
     */
    @Test
    void calendar_ShouldRedirectToLogin_WhenUnauthenticated() {
        driver.get(baseUrl() + "/calendar");
 
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    /**
     * Test if the calendar page displays the correct username 
     */
    @Test
    void calendar_DisplayUsername() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");
 
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("nuno"));
        assertTrue(driver.getPageSource().contains("Your calendar"));
    }

    /**
     * Test the proposal of a meeting and the subsequence appearance in the calendar
     */
    @Test
    void proposeMeeting_Success() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");

        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));

        driver.findElement(By.id("title")).sendKeys("Meeting 1");
        driver.findElement(By.id("description")).sendKeys("Description");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T09:30");

        submitProposalForm();

        wait.until(ExpectedConditions.urlContains("/calendar"));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'Meeting 1')]")));

        assertTrue(driver.getPageSource().contains("Meeting 1"));
    }

    /**
     * Test if an error shows when a new proposed meeting has the end time before the start time
     */
    @Test
    void proposeMeeting_EndBeforeStart() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");
 
        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
 
        driver.findElement(By.id("title")).sendKeys("Meeting 2");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T08:00");
        submitProposalForm();
 
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".error")));
        assertTrue(driver.getPageSource().contains("End time must be after start time"));
    }

    /**
     * Test if accessing /discover without being logged in redirects to the login page
     */
    @Test
    void discover_Unauthenticated() {
        driver.get(baseUrl() + "/discover");

        wait.until(ExpectedConditions.urlContains("/login"));

        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    /**
     * Test if 
     */
    @Test
    void discover_Authenticated() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");

        driver.get(baseUrl() + "/discover");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("q")));

        assertTrue(driver.getPageSource().contains("Discover events"));
        assertTrue(driver.getPageSource().contains("Search public ticketing sites"));
    }

    /**
     * 
     */
    @Test
    void discover_Search() {
        EventProvider mockProvider = mock(EventProvider.class);
        when(mockProvider.isConfigured()).thenReturn(true);
        when(mockProvider.name()).thenReturn("MockProvider");
        when(discoverService.providers()).thenReturn(List.of(mockProvider));

        DiscoveredEvent mockEvent = new DiscoveredEvent(
            "Ticketmaster",
            "ext-123",
            "Jazz Concert",
            "A great jazz show",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            "http://example.com",
            "Lisbon Arena"
        );

        when(discoverService.search(anyString())).thenReturn(List.of(mockEvent));

        registerAndLogin("nuno", "nuno@gmail.pt", "password123");
        driver.get(baseUrl() + "/discover");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("q")));

        WebElement qInput = driver.findElement(By.id("q"));
        qInput.clear();
        qInput.sendKeys("jazz");

        ((JavascriptExecutor) driver).executeScript(
            "document.getElementById('q').value = 'jazz';" +
            "document.querySelector('form[action=\"/discover\"]').submit();"
        );

        wait.until(ExpectedConditions.urlContains("q=jazz"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Jazz Concert"));
    }

    /**
     * Test if the sign-out redirects to the login page
     */
    @Test
    void signOut_RedirectToLogin() {
    registerAndLogin("nuno", "nuno@gmail.pt", "password123");

    ((JavascriptExecutor) driver).executeScript(
        "document.querySelector('form[action*=\"logout\"]').submit();"
    );

    wait.until(ExpectedConditions.urlContains("/login"));
    assertTrue(driver.getCurrentUrl().contains("/login"));
}


    
    
}
