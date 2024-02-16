package com.yakushevso;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class Util {
    public static WebDriver driver;

    public static void createDriver(String visible) {
        // Set path to browser driver
        System.setProperty("webdriver.chrome.driver", SettingsManager.loadSettings().getChromedriver_path());
        ChromeOptions options = new ChromeOptions();

        // Create an instance of the driver in the background if "true"
        if ("visible".equals(visible)) {
            options.addArguments("--start-maximized");
        } else if ("hide".equals(visible)){
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
        }

        driver = new ChromeDriver(options);
    }

    // Perform authorization on the site
    public static void login(String login, String password) {
        driver.get("https://hyperskill.org/login");

        waitDownloadElement("//input[@type='email']");

        WebElement emailField = driver.findElement(By.xpath("//input[@type='email']"));
        WebElement passwordField = driver.findElement(By.xpath("//input[@type='password']"));
        WebElement signInButton = driver.findElement(By.xpath("//button[@data-cy='submitButton']"));

        emailField.sendKeys(login);
        passwordField.sendKeys(password);
        signInButton.click();

        waitDownloadElement("//h1[@data-cy='curriculum-header']");
    }

    // Check if the element has loaded
    public static boolean waitDownloadElement(String xpath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        if (wait.until(ExpectedConditions.and(
                ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)),
                ExpectedConditions.elementToBeClickable(By.xpath(xpath))))) {
            return true;
        } else {
            // Close banner "Do you want to pick up where you left off?"
            closeBanner("//button[@class='btn btn-outline-dark' and text()= 'No, thanks']");

            // Close banner "You probably already know this topic"
            closeBanner("//button[@class='btn btn-outline-dark' and text()= 'Continue with theory']");

            delay(1000);

            return wait.until(ExpectedConditions.and(
                    ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)),
                    ExpectedConditions.elementToBeClickable(By.xpath(xpath))));
        }
    }

    // Close drop-down banner
    private static void closeBanner(String element) {
        try {
            WebElement banner = driver.findElement(By.xpath(element));
            Actions actions = new Actions(driver);
            actions.moveToElement(banner).click().perform();
        } catch (Exception ignored) {
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

    // Close ChromeDriver
    public static void closeDriver() {
        if (driver != null) {
            driver.quit();
        }
    }
}
