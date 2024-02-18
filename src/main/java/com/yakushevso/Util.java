package com.yakushevso;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;

public class Util {
    public static WebDriver createDriver(String visible) {
        // Set path to browser driver
        System.setProperty("webdriver.chrome.driver",
                SettingsManager.loadSettings().getChromedriverPath() + "chromedriver.exe");
        ChromeOptions options = new ChromeOptions();

        // Create an instance of the driver in the background if "true"
        if ("visible".equals(visible)) {
            options.addArguments("--start-maximized");
        } else if ("hide".equals(visible)) {
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
        }

        return new ChromeDriver(options);
    }

    // Perform authorization on the site
    public static void login(WebDriver driver, String login, String password) {
        driver.get("https://hyperskill.org/login");

        waitDownloadElement(driver, "//input[@type='email']");

        WebElement emailField = driver.findElement(By.xpath("//input[@type='email']"));
        WebElement passwordField = driver.findElement(By.xpath("//input[@type='password']"));
        WebElement signInButton = driver.findElement(By.xpath("//button[@data-cy='submitButton']"));

        emailField.sendKeys(login);
        passwordField.sendKeys(password);
        signInButton.click();

        waitDownloadElement(driver, "//h1[@data-cy='curriculum-header']");
    }

    // Check if the element has loaded
    public static boolean waitDownloadElement(WebDriver driver, String xpath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        if (wait.until(ExpectedConditions.and(
                ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)),
                ExpectedConditions.elementToBeClickable(By.xpath(xpath))))) {
            return true;
        } else {
            // Close banner "Do you want to pick up where you left off?"
            closeBanner(driver, "//button[@class='btn btn-outline-dark' and text()= 'No, thanks']");

            // Close banner "You probably already know this topic"
            closeBanner(driver, "//button[@class='btn btn-outline-dark' and text()= 'Continue with theory']");

            delay(1000);

            return wait.until(ExpectedConditions.and(
                    ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)),
                    ExpectedConditions.elementToBeClickable(By.xpath(xpath))));
        }
    }

    // Close drop-down banner
    private static void closeBanner(WebDriver driver, String element) {
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
    public static void closeDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
        }
    }

    // Close all open processes in the system
    public static void closeChromeDriverProcess() {
        try {
            // Check if the chromedriver.exe process is running
            boolean isRunning = false;
            ProcessBuilder checkBuilder = new ProcessBuilder("tasklist");
            Process checkProcess = checkBuilder.start();
            InputStream inputStream = checkProcess.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("chromedriver.exe")) {
                    isRunning = true;
                    break;
                }
            }

            // Kill the process if it is running
            if (isRunning) {
                ProcessBuilder killBuilder = new ProcessBuilder("taskkill", "/F", "/IM", "chromedriver.exe");
                killBuilder.start();
                System.out.println("chromedriver.exe processes found and killed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
