package com.yakushevso;

import com.yakushevso.data.Account;
import com.yakushevso.data.Settings;
import com.yakushevso.data.UserSession;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Scanner;

public class AppLauncher {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        Util.closeChromeDriverProcess();
        SettingsManager.initSettings(SCANNER);
        mainMenu(choiceUser());
    }

    private static void mainMenu(Account account) {
        UserSession userSession = new UserSession();
        userSession.setAccount(account);
        userSession.setTrack(choiceTrack());

        while (true) {
            System.out.println("Current: " + userSession.getAccount().getLogin()
                    + "/" +  userSession.getTrack());

            System.out.println("""
                    Enter mode number:
                    1. Get data
                    2. Save pages
                    3. Get answers
                    4. Send answers
                    5. Settings
                    0. Exit""");

            checkInputNum();
            int mode = SCANNER.nextInt();

            switch (mode) {
                case 1 -> getDataMenu(userSession);
                case 2 -> savePagesMenu(userSession);
                case 3 -> getAnswersMenu(userSession);
                case 4 -> setAnswersMenu(userSession);
                case 5 -> userSession.update(settingsMenu(userSession));
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private static int choiceTrack() {
        System.out.println("Enter track number: ");
        checkInputNum();
        return SCANNER.nextInt();
    }

    private static Account choiceUser() {
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
                return accounts.get(userMode - 1);
            } else if (userMode == 0) {
                return addUser();
            } else {
                System.out.println("Select a user from the list!");
            }
        }
    }

    private static UserSession settingsMenu(UserSession userSession) {
        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Change track
                    2. Change user
                    3. Add user
                    4. Delete user
                    5. Change paths
                    0. Return""");

            checkInputNum();
            int dataMode = SCANNER.nextInt();

            switch (dataMode) {
                case 1 -> {
                    userSession.setTrack(choiceTrack());
                    return userSession;
                }
                case 2 -> {
                    userSession.setAccount(choiceUser());
                    userSession.setTrack(choiceTrack());
                    return userSession;
                }
                case 3 -> {
                    userSession.setAccount(addUser());
                    userSession.setTrack(choiceTrack());
                    return userSession;
                }
                case 4 -> {
                    userSession.setAccount(delUser());
                    userSession.setTrack(choiceTrack());
                    return userSession;
                }
                case 5 -> {
                    changePaths();
                    return userSession;
                }
                case 0 -> {
                    return userSession;
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

    private static Account delUser() {
        Settings settings = SettingsManager.loadSettings();
        List<Account> accounts = settings.getAccounts();

        System.out.println("Select a user: ");
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
                return choiceUser();
            } else {
                System.out.println("Select a user from the list!");
            }
        }
    }

    private static void changePaths() {
        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Change driver path
                    2. Change folder path
                    0. Return""");

            checkInputNum();
            int settingsMode = SCANNER.nextInt();

            Settings settings = SettingsManager.loadSettings();
            System.out.printf("""
                            driver_path: %s
                            driver_path: %s
                            """,
                    settings.getChromedriverPath(),
                    settings.getFolderPath());

            switch (settingsMode) {
                case 1 -> {
                    System.out.println("Enter path ChromeDriver: ");
                    String driver_path = SCANNER.next() + "chromedriver.exe";
                    settings.setChromedriverPath(driver_path);
                    SettingsManager.saveSettings(settings);
                }
                case 3 -> {
                    System.out.println("Enter path FolderPath: ");
                    String folder_path = SCANNER.next() + "TRACK_NUMBER/";
                    settings.setFolderPath(folder_path);
                    SettingsManager.saveSettings(settings);
                }
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private static void getDataMenu(UserSession userSession) {
        Settings settings = SettingsManager.loadSettings();
        DataManager dataManager = new DataManager(userSession, settings);

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
                    WebDriver driver = Util.createDriver("hide");
                    System.out.println("In progress...");
                    Util.login(driver, userSession.getAccount().getLogin(),
                            userSession.getAccount().getPassword());
                    dataManager.getData(driver);
                    dataManager.printStats();
                    Util.closeDriver(driver);
                }
                case 2 -> {
                    WebDriver driver = Util.createDriver("hide");
                    System.out.println("In progress...");
                    Util.login(driver, userSession.getAccount().getLogin(),
                            userSession.getAccount().getPassword());
                    dataManager.printStats();
                    Util.closeDriver(driver);
                }
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private static void savePagesMenu(UserSession userSession) {
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

            WebDriver driver = Util.createDriver("hide");
            System.out.println("In progress...");
            Util.login(driver, userSession.getAccount().getLogin(),
                    userSession.getAccount().getPassword());

            Settings settings = SettingsManager.loadSettings();
            SavePages save = new SavePages(settings);

            switch (saveMode) {
                case 1 -> save.saveTopics(driver);
                case 2 -> save.saveProjects(driver);
                case 3 -> save.saveStages(driver);
                case 4 -> save.saveThemes(driver);
                case 5 -> {
                    save.saveTopics(driver);
                    save.saveProjects(driver);
                    save.saveStages(driver);
                    save.saveThemes(driver);
                }
                default -> System.out.println("Invalid option, please try again.");
            }

            Util.closeDriver(driver);
        }
    }

    private static void getAnswersMenu(UserSession userSession) {
        Settings settings = SettingsManager.loadSettings();
        WebDriver driver = Util.createDriver("hide");
        Automation automation = new Automation(driver, settings);
        System.out.println("In progress...");
        Util.login(driver, userSession.getAccount().getLogin(),
                userSession.getAccount().getPassword());
        automation.getAnswers(userSession.getAccount().getId());
        Util.closeDriver(driver);
    }

    private static void setAnswersMenu(UserSession userSession) {
        Settings settings = SettingsManager.loadSettings();
        WebDriver driver = Util.createDriver("hide");
        Automation automation = new Automation(driver, settings);
        Util.createDriver("visible");
        System.out.println("In progress...");
        Util.login(driver, userSession.getAccount().getLogin(),
                userSession.getAccount().getPassword());
        automation.sendAnswers();
        Util.closeDriver(driver);
    }

    private static void checkInputNum() {
        while (!SCANNER.hasNextInt()) {
            System.out.println("That's not a number. Please enter a number:");
            SCANNER.next();
        }
    }
}