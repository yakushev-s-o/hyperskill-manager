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
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void initSettings() {
        File file = new File(SETTINGS_PATH);

        if (!file.exists() || file.length() == 0) {
            System.out.println("Enter path ChromeDriver: ");
            String driver_path = SCANNER.next() + "chromedriver.exe";
            System.out.println("Enter path FolderPath: ");
            String folder_path = SCANNER.next() + "TRACK_NUMBER/";

            List<Account> accounts = new ArrayList<>();
            Settings settings = new Settings(accounts, driver_path, folder_path,
                    "src/main/resources/answer-list-TRACK_NUMBER.json",
                    "src/main/resources/data-list-TRACK_NUMBER.json",
                    "https://hyperskill.org/");

            saveSettings(settings);
            settings.addAccount(getAccount());
            saveSettings(settings);
        }
    }

    public static Settings loadSettings() {
        try (FileReader reader = new FileReader(SETTINGS_PATH)) {
            return gson.fromJson(reader, Settings.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Cannot load settings", e);
        }
    }

    public static void saveSettings(Settings settings) {
        File file = new File(SETTINGS_PATH);

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Account getAccount() {
        System.out.println("Enter login: ");
        String login = SCANNER.next();
        System.out.println("Enter password: ");
        String password = SCANNER.next();

        Util.createDriver("hide");
        Util.login(login, password);
        int userId = DataManager.getCurrent().get("id").getAsInt();
        Util.closeDriver();

        return new Account(login, password, userId);
    }
}
