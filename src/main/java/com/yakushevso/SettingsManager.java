package com.yakushevso;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yakushevso.data.Account;
import com.yakushevso.data.Settings;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SettingsManager {
    public static void initSettings(Scanner scanner) {
        File file = new File("src/main/resources/settings.json");

        if (!file.exists() || file.length() == 0) {
            System.out.println("Enter path ChromeDriver: ");
            String driverPath = scanner.next();
            System.out.println("Enter path FolderPath: ");
            String folderPath = scanner.next();

            List<Account> accounts = new ArrayList<>();
            Settings settings = new Settings(accounts, driverPath, folderPath);

            saveSettings(settings);
            settings.addAccount(getAccount(scanner));
            saveSettings(settings);
        }
    }

    public static Settings loadSettings() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileReader reader = new FileReader("src/main/resources/settings.json")) {
            return gson.fromJson(reader, Settings.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Cannot load settings", e);
        }
    }

    public static void saveSettings(Settings settings) {
        File file = new File("src/main/resources/settings.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
