# VVS Assignment 2 - Calendar Application Testing
### FCUL — Verificação e Validação de Software (2025/2026)

---

### Overview 

This repository contains the test suite developed for Assignment 2 of the VVS course. The System Under Test (SUT) is Calendar, a Spring Boot web application for managing personal calendar events and discovering events from third-party sources (Ticketmaster, SeatGeek, AgendaLx).

---

### Requirements

- Java 17
- Maven 3.6+
- ChromeDriver (for E2E tests)

---

### Project Structure
**Tests (`src/test/java/com/example/meetings/`)**
```
UnitTestsBusinessLogic/   Unit tests with Mockito
controller/               REST API tests with MockMvc (@WebMvcTest)
discover/                 Third-party provider tests with WireMock
repository/               Database tests with H2 (@DataJpaTest)
e2e/                      End-to-End tests with Selenium + ChromeDriver
```


---

### Test Suite

#### 1. Unit Tests — Business Logic
**Location:** `src/test/java/com/example/meetings/UnitTestsBusinessLogic/`


These tests target the service layer in complete isolation. All dependencies are mocked with Mockito.

| Test Class | Class Under Test |
|---|---|
| `MeetingServiceTest` | `MeetingService` |
| `UserServiceTest` | `UserService` | 
| `ICalServiceTest` | `ICalService` | 
| `DiscoveryServiceTest` | `DiscoveryService` | 
| `AppUserDetailsServiceTest` | `AppUserDetailsService` |


**Run with:**
```bash
mvn test
```
 
---

#### 2. Integration Tests — Third-Party Providers
**Location:** `src/test/java/com/example/meetings/discover/`

These tests verify the integration with external event APIs. A **WireMock** server stubs the HTTP responses so that tests are deterministic and do not require real API keys.

| Test Class | Provider |
|---|---|
| `TicketmasterProviderTest` | Ticketmaster API |
| `SeatGeekProviderTest` | SeatGeek API |
| `AgendaLxProviderTest` | AgendaLx (Lisboa) |

**Run with:**
```bash
mvn verify -Pintegration-tests
```
 
---

#### 3. Integration Tests — REST API Level
**Location:** `src/test/java/com/example/meetings/controller/`
 
These tests use Spring's **`@WebMvcTest`** + **`MockMvc`** to exercise the full request/response cycle at the HTTP layer, while mocking the service layer.
 
| Test Class | Controller |
|---|---|
| `AuthControllerTest` | `AuthController` (register/login) |
| `CalendarControllerTest` | `CalendarController` (calendar view) |
| `MeetingControllerTest` | `MeetingController` (create/accept/reject meetings) |
| `DiscoveryControllerTest` | `DiscoveryController` (event discovery + import) |
| `ICalControllerTest` | `ICalController` (iCal feed export) |
 
**Run with:**
```bash
mvn verify -Pintegration-tests
```
 
---

#### 4. Integration Tests — Database
**Location:** `src/test/java/com/example/meetings/repository/`
 
These tests use **`@DataJpaTest`** with an in-memory H2 database to validate the persistence layer against a real (but isolated) database.
 
| Test Class | Repository |
|---|---|
| `MeetingRepositoryTest` | `MeetingRepository` |
| `UserRepositoryTest` | `UserRepository` |
| `MeetingParticipantRepositoryTest` | `MeetingParticipantRepository` |
 
**Run with:**
```bash
mvn verify -Pintegration-tests
```
 
---
 
#### 5. End-to-End Tests
**Location:** `src/test/java/com/example/meetings/e2e/`
 
End-to-end tests using **Selenium WebDriver** that drive a real browser against a fully booted application with a dedicated test database. These tests simulate real user flows from login to event creation.
 
**Run with:**
```bash
mvn verify -Pe2e-tests
```
 
---

### Continuous Integration
 
A GitHub Actions workflow is configured at `.github/workflows/ci.yml`. It runs on every push and pull request to `main`, executing all three test phases in sequence:
 
```
Unit Tests → Integration Tests → E2E Tests
```
 
The pipeline uses **JDK 17 (Temurin)** on `ubuntu-latest`.






