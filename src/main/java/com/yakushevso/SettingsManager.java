package com.yakushevso;

import com.yakushevso.data.Account;
import com.yakushevso.data.Settings;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private static final String settingsPath = "src/main/resources/settings.json";
    private static final Logger log = LoggerFactory.getLogger(SettingsManager.class);

    public static void initSettings() {
        log.info("Initializing settings...");

        File file = new File(settingsPath);

        if (!file.exists() || file.length() == 0) {
            log.info("The settings file was not found or is empty, creating a new one...");

            System.out.println("Enter path ChromeDriver: ");
            String driverPath = Util.getScanner().next();
            log.debug("ChromeDriver path entered: {}", driverPath);
            System.out.println("Enter path FolderPath: ");
            String folderPath = Util.getScanner().next();
            log.debug("FolderPath entered: {}", folderPath);

            List<Account> accounts = new ArrayList<>();
            Settings settings = new Settings(accounts, driverPath, folderPath);

            try {
                DataManager.saveToFile(settings, settingsPath);
                settings.getAccounts().add(getAccount());
                DataManager.saveToFile(settings, settingsPath);
                log.info("New settings file created successfully.");
            } catch (Exception e) {
                log.error("Error saving settings to file: {}", e.getMessage(), e);
            }
        } else {
            log.info("The settings file exists, proceeding without creating a new one.");
        }
    }

    public static Settings loadSettings() {
        log.debug("Loading settings from path: {}", settingsPath);
        Settings settings = DataManager.getFileData(Settings.class, settingsPath);

        if (settings != null) {
            log.info("Settings successfully loaded from {}", settingsPath);
        } else {
            log.error("The settings were not found, the process of creating default settings...");
            initSettings();
        }

        return settings;
    }

    public static void saveSettings(Settings settings) {
        DataManager.saveToFile(settings, settingsPath);
        log.debug("The settings are successfully saved.");
    }

    public static Account getAccount() {
        log.debug("The process of obtaining an account...");

        String login;
        String password;
        int userId;

        while (true) {
            System.out.println("Enter login: ");
            login = Util.getScanner().next();
            log.debug("Login entered: {}", login);
            System.out.println("Enter password: ");
            password = Util.getScanner().next();
            log.debug("Login entered: *****");

            try {
                WebDriver driver = Util.createDriver(false);
                Util.login(driver, login, password);
                userId = DataManager.getCurrent(driver).get("id").getAsInt();
                log.info("Account data obtained successfully. User ID: {}", userId);
                Util.closeDriver(driver);
                break;
            } catch (Exception e) {
                System.out.println("Login or password is incorrect. Please try again.");
            }
        }

        log.info("Account creation successful for login: {}", login);
        return new Account(login, password, userId);
    }
}
