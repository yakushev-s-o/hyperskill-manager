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
    private static Gson gson = new Gson();
    private static String driver_path;

    public SettingsManager() {
        File file = new File(SETTINGS_PATH);

        if (!file.exists() || file.length() == 0) {
            System.out.println("Enter login: ");
            String login = SCANNER.next();
            System.out.println("Enter password: ");
            String password = SCANNER.next();
            System.out.println("Enter path ChromeDriver: ");
            driver_path = SCANNER.next();
            System.out.println("Enter path FolderPath: ");
            String folder_path = SCANNER.next();
            System.out.println("Enter path JsonPath: ");
            String json_path = SCANNER.next();
            System.out.println("Enter path DataPath: ");
            String data_path = SCANNER.next();

            Util.createDriver("hide");
            Util.login(login, password);
            int userId = DataManager.getCurrent().get("id").getAsInt();
            Util.closeDriver();

            List<Account> account = new ArrayList<>();
            account.add(new Account(login, password, userId));
            Settings settings = new Settings(account, driver_path,
                    folder_path, json_path, data_path, "https://hyperskill.org/"
            );

            gson = new GsonBuilder().setPrettyPrinting().create();

            try {
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(settings, writer);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String getDriver_path() {
        return driver_path;
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

        gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addAccount() {
        Settings settings = loadSettings();

        if (settings != null) {
            System.out.println("Enter login: ");
            String login = SCANNER.next();
            System.out.println("Enter password: ");
            String password = SCANNER.next();

            Util.createDriver("hide");
            Util.login(login, password);
            int userId = DataManager.getCurrent().get("id").getAsInt();
            Util.closeDriver();

            settings.setAccounts(login, password, userId);

            saveSettings(settings);
        }
    }
}
