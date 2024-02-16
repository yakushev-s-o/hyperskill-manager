package com.yakushevso;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yakushevso.data.Account;
import com.yakushevso.data.Settings;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SettingsManager {
    private static final String SETTINGS_PATH = "src/main/resources/settings.json";

    public static void initSettings(Scanner scanner) {
        File file = new File(SETTINGS_PATH);

        if (!file.exists() || file.length() == 0) {
            System.out.println("Enter path ChromeDriver: ");
            String driver_path = scanner.next() + "chromedriver.exe";
            System.out.println("Enter path FolderPath: ");
            String folder_path = scanner.next() + "TRACK_NUMBER/";

            List<Account> accounts = new ArrayList<>();
            Settings settings = new Settings(accounts, driver_path, folder_path,
                    "src/main/resources/answer-list-TRACK_NUMBER.json",
                    "src/main/resources/data-list-TRACK_NUMBER.json",
                    "https://hyperskill.org/");

            saveSettings(settings);
            settings.addAccount(getAccount(scanner));
            saveSettings(settings);
        }
    }

    public static Settings loadSettings() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileReader reader = new FileReader(SETTINGS_PATH)) {
            return gson.fromJson(reader, Settings.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Cannot load settings", e);
        }
    }

    public static void saveSettings(Settings settings) {
        File file = new File(SETTINGS_PATH);
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
                Util.createDriver("hide");
                Util.login(login, password);
                userId = new DataManager(Util.getDriver(),
                        loadSettings()).getCurrent().get("id").getAsInt();
                Util.closeDriver();
                break;
            } catch (Exception e) {
                System.out.println("Login or password is incorrect. Please try again.");
            }
        }

        return new Account(login, password, userId);
    }
}
