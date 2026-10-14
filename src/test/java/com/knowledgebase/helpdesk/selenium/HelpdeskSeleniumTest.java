package com.knowledgebase.helpdesk.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class HelpdeskSeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String baseUrl =
            System.getProperty("baseUrl", "http://localhost:8082");

    @BeforeEach
    void setUp() {
        boolean enabled = Boolean.parseBoolean(
                System.getProperty("selenium.enabled", "false")
        );

        Assumptions.assumeTrue(
                enabled,
                "Selenium tests disabled"
        );

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1440,900");

        String remoteUrl = System.getProperty("selenium.remoteUrl");
        if ((remoteUrl != null && !remoteUrl.isBlank())
                || Boolean.getBoolean("selenium.headless")) {
            options.addArguments(
                    "--headless=new",
                    "--no-sandbox",
                    "--disable-dev-shm-usage"
            );
        }

        if (remoteUrl != null && !remoteUrl.isBlank()) {
            try {
                driver = new RemoteWebDriver(new URL(remoteUrl), options);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(
                        "Invalid Selenium remote URL: " + remoteUrl,
                        e
                );
            }
        } else {
            driver = new ChromeDriver(options);
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    private void login(String username, String password) {

        driver.get(baseUrl + "/login");

        var usernameField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.name("username")
                )
        );

        var passwordField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.name("password")
                )
        );

        usernameField.clear();
        usernameField.sendKeys(username);
        passwordField.clear();
        passwordField.sendKeys(password);

        String actualUsername = usernameField.getAttribute("value");
        String actualPassword = passwordField.getAttribute("value");

        if (!username.equals(actualUsername)
                || !password.equals(actualPassword)) {
            throw new AssertionError(
                    "Login field values could not be populated correctly."
            );
        }

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(currentDriver ->
                currentDriver.getCurrentUrl().contains("/dashboard")
                        || currentDriver.findElements(
                                By.cssSelector(".error")
                        ).stream().anyMatch(WebElement::isDisplayed)
        );

        String loginError = driver.findElements(By.cssSelector(".error"))
                .stream()
                .filter(WebElement::isDisplayed)
                .map(WebElement::getText)
                .collect(Collectors.joining("; "));

        assertTrue(
                driver.getCurrentUrl().contains("/dashboard"),
                () -> "Login did not reach the dashboard. "
                        + (loginError.isBlank()
                        ? "No login error was displayed."
                        : "Application response: " + loginError)
        );
    }

    @Test
    void userCanLoginSuccessfully() {

        login("user", "user123");

        assertTrue(
                driver.getPageSource().contains("Dashboard")
        );

        assertTrue(
                driver.getPageSource().contains("user")
        );
    }

    @Test
    void userCanSearchKnowledgeArticle() {

        login("user", "user123");

        driver.get(baseUrl + "/articles");

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.name("keyword")
                )
        );

        driver.findElement(By.name("keyword"))
                .sendKeys("password");

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.linkText("Password Reset Guide")
                )
        );

        assertTrue(
                driver.findElements(
                        By.linkText("Password Reset Guide")
                ).size() > 0
        );
    }

    @Test
    void supportStaffCanCreateArticle() {

        login("support", "support123");

        driver.get(baseUrl + "/articles/new");

        String title =
                "Selenium Test Article "
                        + System.currentTimeMillis();

        driver.findElement(By.name("title"))
                .sendKeys(title);

        driver.findElement(By.name("description"))
                .sendKeys(
                        "Automated Selenium test problem."
                );

        driver.findElement(By.name("solution"))
                .sendKeys(
                        "Automated Selenium test solution."
                );

        Select status =
                new Select(
                        driver.findElement(By.name("status"))
                );

        status.selectByValue("PUBLISHED");

        driver.findElement(
                By.cssSelector("button[type='submit']")
        ).click();

        wait.until(
                ExpectedConditions.urlContains("/articles")
        );

        assertTrue(
                driver.getPageSource().contains(title)
        );
    }

    @Test
    void normalUserCannotAccessCreateArticle() {

        login("user", "user123");

        driver.get(baseUrl + "/articles/new");

        wait.until(
                ExpectedConditions.urlContains("/articles")
        );

        assertFalse(
                driver.getPageSource()
                        .contains("Create Knowledge Article")
        );

        driver.get(baseUrl + "/articles");

        assertEquals(
                0,
                driver.findElements(
                        By.linkText("+ Create Article")
                ).size()
        );
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {

        if (driver == null) {
            return;
        }

        try {

            Files.createDirectories(
                    Paths.get("test-output")
            );

            String fileName =
                    testInfo.getTestMethod()
                            .map(method -> method.getName())
                            .orElse("selenium-test");

            Path destination =
                    Paths.get(
                            "test-output",
                            fileName + ".png"
                    );

            Files.copy(
                    ((TakesScreenshot) driver)
                            .getScreenshotAs(
                                    OutputType.FILE
                            )
                            .toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            System.out.println(
                    "Could not save Selenium screenshot: "
                            + e.getMessage()
            );

        } finally {

            driver.quit();
        }
    }
}
