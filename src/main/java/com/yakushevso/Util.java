package com.yakushevso;

import com.yakushevso.data.Settings;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Scanner;

public class Util {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    public static Scanner getScanner() {
        return SCANNER;
    }

    public static WebDriver createDriver(boolean visible) {
        log.debug("The process of creating a WebDriver. Visibility mode: {}", visible);

        // Set path to browser driver
        Settings settings = SettingsManager.loadSettings();
        String chromedriverPath = settings.getChromedriverPath();
        System.setProperty("webdriver.chrome.driver", chromedriverPath + "chromedriver.exe");
        log.debug("ChromeDriver path set to: {}", chromedriverPath + "chromedriver.exe");

        ChromeOptions options = new ChromeOptions();

        // Create an instance of the driver in the background if "true"
        if (visible) {
            options.addArguments("--start-maximized");
            log.debug("WebDriver is set to visible mode.");
        } else {
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            log.debug("WebDriver is set to headless mode.");
        }

        log.info("WebDriver created successfully. Mode: {}", visible);
        return new ChromeDriver(options);
    }

    // Close ChromeDriver
    public static void closeDriver(WebDriver driver) {
        if (driver != null) {
            try {
                log.debug("Attempting to close the WebDriver.");
                driver.quit();
                log.info("WebDriver closed successfully.");
            } catch (Exception e) {
                log.error("Failed to close the WebDriver: {}", e.getMessage(), e);
            }
        } else {
            log.warn("WebDriver is null, it might have been closed already or not initialized.");
        }
    }

    // Perform authorization on the site
    public static void login(WebDriver driver, String login, String password) {
        try {
            log.info("The login process. User: {}", login);
            driver.get("https://hyperskill.org/login");
            waitDownloadElement(driver, "//input[@type='email']");

            WebElement emailField = driver.findElement(By.xpath("//input[@type='email']"));
            WebElement passwordField = driver.findElement(By.xpath("//input[@type='password']"));
            WebElement signInButton = driver.findElement(By.xpath("//button[@data-cy='submitButton']"));

            emailField.sendKeys(login);
            passwordField.sendKeys(password);

            signInButton.click();
            waitDownloadElement(driver, "//div[@class='tw-flex-1 -tw-mt-px']");
            log.info("Successfully logged in. User: {}", login);
        } catch (Exception e) {
            log.error("Account login error: {}", e.getMessage(), e);
        }
    }

    // Check if the element has loaded
    public static boolean waitDownloadElement(WebDriver driver, String xpath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            log.debug("Waiting for element to be ready: {}", xpath);
            wait.until(ExpectedConditions.and(
                    ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)),
                    ExpectedConditions.elementToBeClickable(By.xpath(xpath))));
            log.info("Element found and ready: {}", xpath);
            return true;
        } catch (Exception e) {
            log.warn("Element not found within the time frame, attempting to close banners and retry: {}", xpath, e);
            // Attempt to close banners if they appear and interfere with the loading of the element
            closeBanner(driver, "//button[@class='btn btn-outline-dark' and text()= 'No, thanks']");
            closeBanner(driver, "//button[@class='btn btn-outline-dark' and text()= 'Continue with theory']");
            // Additional delay before retry
            delay(1000);
            // Repeated attempt to find an element after closing banners and delay
            try {
                wait.until(ExpectedConditions.and(
                        ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)),
                        ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)),
                        ExpectedConditions.elementToBeClickable(By.xpath(xpath))));
                log.info("Element found and ready after retry: {}", xpath);
                return true;
            } catch (Exception e2) {
                log.error("Element still not found after retry: {}", xpath, e2);
                return false;
            }
        }
    }

    // Close drop-down banner
    private static void closeBanner(WebDriver driver, String element) {
        try {
            WebElement banner = driver.findElement(By.xpath(element));
            Actions actions = new Actions(driver);
            actions.moveToElement(banner).click().perform();
            log.debug("Banner closed successfully. XPath: {}", element);
        } catch (NoSuchElementException e) {
            log.debug("Banner not found, nothing to close. XPath: {}", element);
        } catch (Exception e) {
            log.error("Unexpected error occurred while trying to close banner. XPath: {}. Error: {}", element, e.getMessage(), e);
        }
    }

    // Delay between transitions
    public static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Close all open processes in the system
    public static void closeChromeDriverProcess() {
        try {
            log.info("Search for running processes 'chromedriver.exe...'");
            boolean isRunning = false;
            ProcessBuilder checkBuilder = new ProcessBuilder("tasklist");
            Process checkProcess = checkBuilder.start();
            InputStream inputStream = checkProcess.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("chromedriver.exe")) {
                    log.debug("'chromedriver.exe' process found.");
                    isRunning = true;
                    break;
                }
            }

            // Kill the process if it is running
            if (isRunning) {
                ProcessBuilder killBuilder = new ProcessBuilder("taskkill", "/F", "/IM", "chromedriver.exe");
                killBuilder.start();
                log.info("Running processes 'chromedriver.exe' found and killed.");
            } else {
                log.info("Running processes 'chromedriver.exe' were not found.");
            }
        } catch (Exception e) {
            log.error("An unexpected error occurred while closing 'chromedriver.exe': {}", e.getMessage(), e);
        }
    }
}
