package com.example.meetings.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.DiscoveryService;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;

/**
 * End-to-End tests using Selenium WebDriver and a dedicated test database
 * Chrome runs in headless mode
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:file:./target/e2edb;AUTO_SERVER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.base-url=http://localhost"
})
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
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
            // 1. Change type to text to completely disable HTML5 browser validation
            "arguments[0].type = 'text';" + 
            // 2. Set the value
            "arguments[0].value = arguments[1];", 
            el, value
        );
    }

    private void submitProposalForm() {
        ((JavascriptExecutor) driver).executeScript(
            "document.querySelector('form[action*=\"meetings\"]').submit();"
        );
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
        System.out.println(driver.getCurrentUrl());
        System.out.println(driver.getPageSource());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
 
        driver.findElement(By.id("title")).sendKeys("Meeting 1");
        driver.findElement(By.id("description")).sendKeys("Description");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T09:30");
        submitProposalForm();

 
        wait.until(ExpectedConditions.urlContains("/calendar"));
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
     * Test if an invited user sees a pending invite on their calendar and can accept it
     */
    @Test
    void pendingInvite_ShouldAppearAndBeAccepted() {
        register("organizer", "organizer@gmail.pt", "password123");
        register("invitee", "invitee@gmail.pt", "password123");
 
        // Organizer proposes a meeting and invites the invitee
        login("organizer", "password123");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Meeting 3");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T09:30");
        driver.findElement(By.id("invitees")).sendKeys("invitee");
        submitProposalForm();
        wait.until(ExpectedConditions.urlContains("/calendar"));

        // Create a completly fresh WebDriver session
        driver.quit();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Invitee logs in and sees the pending invite
        login("invitee", "password123");
        assertTrue(driver.getPageSource().contains("Meeting 3"));
        assertTrue(driver.getPageSource().contains("pending"));
 
        // Invitee accepts
        WebElement acceptButton = driver.findElement(
                By.xpath("//input[@value='accept']/following-sibling::button | //input[@name='action'][@value='accept']/../button"));
        acceptButton.click();
 
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("Meeting 3"));
    }

    /**
     * Test if a pending invite disappears after an invited user declines it
     */
    @Test
    void pendingInvite_Decline() {
        register("organizer2", "organizer2@gmail.pt", "password123");
        register("invitee2", "invitee2@gmail.pt", "password123");
 
        // Organizer proposes a meeting and invites the invitee
        login("organizer2", "password123");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        driver.get(baseUrl() + "/meetings/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
        driver.findElement(By.id("title")).sendKeys("Meeting 4");
        setDateTimeLocal("start", "2027-06-15T09:00");
        setDateTimeLocal("end", "2027-06-15T09:30");
        driver.findElement(By.id("invitees")).sendKeys("invitee2");
        submitProposalForm();
        wait.until(ExpectedConditions.urlContains("/calendar"));
 
        // Invitee declines
        login("invitee2", "password123");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains("Meeting 4"));
 
        WebElement declineButton = driver.findElement(
                By.xpath("//input[@name='action'][@value='decline']/../button"));
        declineButton.click();
 
        wait.until(ExpectedConditions.urlContains("/calendar"));
        // After declining, the meeting should not appear in the calendar
        assertFalse(driver.getPageSource().contains("Meeting 4"));
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
    void discover_Search_ShouldShowResults() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");

        driver.get(baseUrl() + "/discover");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("q")));

        driver.findElement(By.id("q")).sendKeys("jazz");

        driver.findElement(By.cssSelector("button[type=submit]")).click();

        System.out.println(driver.getPageSource());

        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//*[contains(text(),'Results for')]")
        ));

        assertTrue(driver.getPageSource().contains("Results for"));
    }

    /**
     * Test if the sign-out redirects to the login page
     */
    @Test
    void signOut_RedirectToLogin() {
        registerAndLogin("nuno", "nuno@gmail.pt", "password123");
 
        driver.findElement(By.xpath("//button[text()='Sign out']")).click();
 
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }


    
    
}
