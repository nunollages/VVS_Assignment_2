package com.example.meetings.e2e;

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
import org.springframework.test.context.TestPropertySource;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.DiscoveryService;
import com.example.meetings.discover.EventProvider;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;

/**
 * End-to-End tests using Selenium WebDriver with Chrome in headless mode.
 * 
 * A isolated in-memory H2 database is used
 * 
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @MockBean
    private DiscoveryService discoverService;
 
    private WebDriver driver;
    private WebDriverWait wait;
 
    private String baseUrl() {
        return "http://localhost:" + port;
    }
 
    @BeforeEach
    void setUp() {
        // Clean database
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        // Launch Chrome in headless mode
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }
 
    @AfterEach
    void tearDown() {
        // Clear cookies and close the browser
        if (driver != null) {
            try {
                driver.manage().deleteAllCookies();
            } catch (Exception ignored) {}
            driver.quit();
        }
    }

    // HELPERS

    /**
     * Registers a user and waits for the redirect to the login page
     * 
     * @param username
     * @param email 
     * @param password
     */
    private void register(String username, String email, String password) {
        driver.get(baseUrl() + "/register");
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type=submit]")).click();
        wait.until(ExpectedConditions.urlContains("/login"));

    }

    /**
     * Navigates to the login pages and fills the form with the given credentials
     * 
     * @param username
     * @param password
     */
    private void login(String username, String password) {
        driver.get(baseUrl() + "/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        // Clear the fields to avoid any previous inputs
        usernameField.clear();
        passwordField.clear();

        usernameField.sendKeys(username);
        passwordField.sendKeys(password);

        WebElement submit =
            driver.findElement(By.cssSelector("button[type=submit]"));

        wait.until(ExpectedConditions.elementToBeClickable(submit));

        submit.click();

        // Waits until the URL contains either /calendar (success) or "error" (bad credentials)
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/calendar"),
            ExpectedConditions.urlContains("error")
        ));
    }

    /**
     * Registers a user and immediately logs in
     * 
     * @param username
     * @param email
     * @param password
     */
    private void registerAndLogin(String username, String email, String password) {
        register(username, email, password);
        wait.until(ExpectedConditions.urlContains("/login"));
        login(username, password);
        wait.until(ExpectedConditions.urlContains("/calendar"));
    }

    /**
     * This function was created because Selenium's sendKeys cannot reliably type into datetime-local
     * inputs
     * It sets the value of a "datetime-local" input via JavaScript
     */
    private void setDateTimeLocal(String id, String value) {
        WebElement el = driver.findElement(By.id(id));
        ((JavascriptExecutor) driver).executeScript(
            "var el = arguments[0];" +
            "el.value = arguments[1];" +
            "el.dispatchEvent(new Event('input', {bubbles: true}));" +
            "el.dispatchEvent(new Event('change', {bubbles: true}));",
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

        // Submit via fetch, which correctly sends all form fields
        ((JavascriptExecutor) driver).executeScript(
            "var form = document.querySelector('form[action*=\"/meetings/new\"]');" +
            "var data = new FormData(form);" +
            "data.set('title', 'Meeting 1');" +
            "data.set('description', 'Description');" +
            "data.set('start', '2027-06-15T09:00');" +
            "data.set('end', '2027-06-15T09:30');" +
            "fetch(form.action, {method:'POST', body:data})" +
            "  .then(r => window.location = '/calendar');"
        );

        wait.until(ExpectedConditions.urlContains("/calendar"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[contains(text(),'Your calendar')]")
        ));
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//*[contains(text(), 'Meeting 1')]")
        ));
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
     * Test if an authenticated user can successfully access the discover page 
     * and see the search interface elements
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
     * Test if a user can submit a search query in the discover page and if the 
     * mocked results from the DiscoveryService are correctly rendered on the screen
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
            "Bad bunny World Tour",
            "Bad bunny World Tour",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            "http://example.com",
            "Estádio do Benfica"
        );

        when(discoverService.search(anyString())).thenReturn(List.of(mockEvent));

        registerAndLogin("nuno", "nuno@gmail.pt", "password123");
        driver.get(baseUrl() + "/discover");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("q")));

        WebElement qInput = driver.findElement(By.id("q"));

        // Search for the mocked event
        qInput.clear();
        qInput.sendKeys("bad bunny");

        ((JavascriptExecutor) driver).executeScript(
            "document.getElementById('q').value = 'bad bunny';" +
            "document.querySelector('form[action=\"/discover\"]').submit();"
        );

        wait.until(ExpectedConditions.urlContains("q=bad+bunny"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Bad bunny World Tour"));
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
