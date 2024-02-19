package com.yakushevso;

import com.yakushevso.data.Account;
import com.yakushevso.data.Settings;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SettingsManager {
    private static final String settingsPath = "src/main/resources/settings.json";

    public static void initSettings(Scanner scanner) {
        File file = new File(settingsPath);

        if (!file.exists() || file.length() == 0) {
            System.out.println("Enter path ChromeDriver: ");
            String driverPath = scanner.next();
            System.out.println("Enter path FolderPath: ");
            String folderPath = scanner.next();

            List<Account> accounts = new ArrayList<>();
            Settings settings = new Settings(accounts, driverPath, folderPath);

            DataManager.saveToFile(settings, settingsPath);
            settings.addAccount(getAccount(scanner));
            DataManager.saveToFile(settings, settingsPath);
        }
    }

    public static Settings loadSettings() {
        return DataManager.getFileData(Settings.class, settingsPath);
    }

    public static void saveSettings(Settings settings) {
        DataManager.saveToFile(settings, settingsPath);
    }

    public static Account getAccount(Scanner scanner) {
        String login;
        String password;
        int userId;

        while (true) {
            System.out.println("Enter login: ");
            login = scanner.next();
            System.out.println("Enter password: ");
            password = scanner.next();

            try {
                WebDriver driver = Util.createDriver("hide");
                Util.login(driver, login, password);
                userId = DataManager.getCurrent(driver).get("id").getAsInt();
                Util.closeDriver(driver);
                break;
            } catch (Exception e) {
                System.out.println("Login or password is incorrect. Please try again.");
            }
        }

        return new Account(login, password, userId);
    }
}
