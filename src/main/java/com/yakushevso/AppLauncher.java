package com.yakushevso;

import com.yakushevso.data.Account;
import com.yakushevso.data.Settings;

import java.util.List;
import java.util.Scanner;

public class AppLauncher {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static int track;
    private static Account user;

    public static void main(String[] args) {
        Util.closeChromeDriverProcess();
        SettingsManager.initSettings(SCANNER);
        choiceUser();
        mainMenu();
    }

    private static void mainMenu() {
        choiceTrack();

        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Get data
                    2. Save pages
                    3. Get answers
                    4. Send answers
                    5. Change track
                    6. Settings
                    0. Exit""");

            checkInputNum();
            int mode = SCANNER.nextInt();

            switch (mode) {
                case 1 -> getDataMenu();
                case 2 -> savePagesMenu();
                case 3 -> getAnswersMenu();
                case 4 -> setAnswersMenu();
                case 5 -> choiceTrack();
                case 6 -> settingsMenu();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private static void choiceTrack() {
        System.out.println("Enter track number: ");
        checkInputNum();
        track = SCANNER.nextInt();
    }

    private static void choiceUser() {
        System.out.println("Select a user: ");
        List<Account> accounts = SettingsManager.loadSettings().getAccounts();

        for (int i = 1; i <= accounts.size(); i++) {
            System.out.println(i + ". " + accounts.get(i - 1).getLogin());
        }

        System.out.println("0. Add new user");

        while (true) {
            checkInputNum();
            int userMode = SCANNER.nextInt();

            if (userMode <= accounts.size() && userMode != 0) {
                user = accounts.get(userMode - 1);
                return;
            } else if (userMode == 0) {
                user = addUser();
                return;
            } else {
                System.out.println("Select a user from the list!");
            }
        }
    }

    private static void settingsMenu() {
        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Change user
                    2. Add user
                    3. Delete user
                    4. Change paths
                    0. Return""");

            checkInputNum();
            int dataMode = SCANNER.nextInt();

            switch (dataMode) {
                case 1 -> {
                    choiceUser();
                    mainMenu();
                }
                case 2 -> addUser();
                case 3 -> {
                    delUser();
                    return;
                }
                case 4 -> changePaths();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private static Account addUser() {
        Settings settings = SettingsManager.loadSettings();
        Account account = SettingsManager.getAccount(SCANNER);
        settings.addAccount(account);
        SettingsManager.saveSettings(settings);
        System.out.println("Account \"" + account.getLogin() + "\" added!");

        return account;
    }

    private static void delUser() {
        System.out.println("Select a user: ");
        Settings settings = SettingsManager.loadSettings();
        List<Account> accounts = settings.getAccounts();

        for (int i = 1; i <= accounts.size(); i++) {
            System.out.println(i + ". " + accounts.get(i - 1).getLogin());
        }

        while (true) {
            checkInputNum();
            int userMode = SCANNER.nextInt();

            if (userMode <= accounts.size() && userMode != 0) {
                System.out.println("Account \"" + accounts.get(userMode - 1).getLogin() + "\" deleted!");
                settings.delAccount(userMode - 1);
                SettingsManager.saveSettings(settings);
                choiceUser();
                return;
            } else {
                System.out.println("Select a user from the list!");
            }
        }
    }

    private static void changePaths() {
        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Show paths
                    2. Change driver path
                    3. Change folder path
                    0. Return""");

            checkInputNum();
            int settingsMode = SCANNER.nextInt();

            Settings settings = SettingsManager.loadSettings();

            switch (settingsMode) {
                case 1 -> System.out.printf("""
                                driver_path: %s
                                driver_path: %s
                                """,
                        settings.getChromedriver_path(),
                        settings.getFolder_path());
                case 2 -> {
                    System.out.println("Enter path ChromeDriver: ");
                    String driver_path = SCANNER.next() + "chromedriver.exe";
                    settings.setChromedriver_path(driver_path);
                    SettingsManager.saveSettings(settings);
                }
                case 3 -> {
                    System.out.println("Enter path FolderPath: ");
                    String folder_path = SCANNER.next() + "TRACK_NUMBER/";
                    settings.setFolder_path(folder_path);
                    SettingsManager.saveSettings(settings);
                }
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private static void getDataMenu() {
        Settings settings = SettingsManager.loadSettings();
        DataManager dataManager = new DataManager(Util.getDriver(), settings);

        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Update data
                    2. Get last statistic
                    0. Return""");

            checkInputNum();
            int dataMode = SCANNER.nextInt();

            switch (dataMode) {
                case 1 -> {
                    Util.createDriver("hide");
                    System.out.println("In progress...");
                    Util.login(user.getLogin(), user.getPassword());
                    dataManager.getData(track);
                    dataManager.printStats();
                    Util.closeDriver();
                }
                case 2 -> {
                    Util.createDriver("hide");
                    System.out.println("In progress...");
                    Util.login(user.getLogin(), user.getPassword());
                    dataManager.printStats();
                }
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private static void savePagesMenu() {
        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Save topics
                    2. Save projects
                    3. Save stages
                    4. Save themes
                    5. Save all
                    0. Return""");

            checkInputNum();
            int saveMode = SCANNER.nextInt();

            if (saveMode == 0) {
                return;
            }

            Util.createDriver("hide");
            System.out.println("In progress...");
            Util.login(user.getLogin(), user.getPassword());

            Settings settings = SettingsManager.loadSettings();
            SavePages save = new SavePages(Util.getDriver(), settings);

            switch (saveMode) {
                case 1 -> save.saveTopics();
                case 2 -> save.saveProjects();
                case 3 -> save.saveStages();
                case 4 -> save.saveThemes();
                case 5 -> {
                    save.saveTopics();
                    save.saveProjects();
                    save.saveStages();
                    save.saveThemes();
                }
                default -> System.out.println("Invalid option, please try again.");
            }

            Util.closeDriver();
        }
    }

    private static void getAnswersMenu() {
        Settings settings = SettingsManager.loadSettings();
        Automation automation = new Automation(Util.getDriver(), settings);
        Util.createDriver("hide");
        System.out.println("In progress...");
        Util.login(user.getLogin(), user.getPassword());
        automation.getAnswers(user.getId());
        Util.closeDriver();
    }

    private static void setAnswersMenu() {
        Settings settings = SettingsManager.loadSettings();
        Automation automation = new Automation(Util.getDriver(), settings);
        Util.createDriver("visible");
        System.out.println("In progress...");
        Util.login(user.getLogin(), user.getPassword());
        automation.sendAnswers();
        Util.closeDriver();
    }

    private static void checkInputNum() {
        while (!SCANNER.hasNextInt()) {
            System.out.println("That's not a number. Please enter a number:");
            SCANNER.next();
        }
    }
}