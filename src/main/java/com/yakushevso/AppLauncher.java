package com.yakushevso;

import com.yakushevso.data.Account;
import com.yakushevso.data.Settings;
import com.yakushevso.data.UserSession;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AppLauncher {
    private static final Logger log = LoggerFactory.getLogger(AppLauncher.class);

    public static void main(String[] args) {
        Util.closeChromeDriverProcess();
        SettingsManager.initSettings();
        mainMenu(choiceUser());
    }

    private static void mainMenu(Account account) {
        UserSession userSession = new UserSession();
        userSession.setAccount(account);
        userSession.setTrack(choiceTrack());
        log.info("Entering main menu for account: {}/{}", account.login(), userSession.getTrack());

        while (true) {
            System.out.println("Current: " + userSession.getAccount().login()
                    + "/" + userSession.getTrack());

            System.out.println("""
                    Enter mode number:
                    1. Get data
                    2. Save pages
                    3. Get answers
                    4. Send answers
                    5. Settings
                    0. Exit""");

            checkInputNum();
            int mode = Util.getScanner().nextInt();
            log.debug("User selected mode: {}", mode);

            switch (mode) {
                case 1 -> getDataMenu(userSession);
                case 2 -> savePagesMenu(userSession);
                case 3 -> getAnswersMenu(userSession);
                case 4 -> setAnswersMenu(userSession);
                case 5 -> {
                    UserSession userSessionNew = settingsMenu(userSession);
                    userSession.setTrack(userSessionNew.getTrack());
                    userSession.setAccount(userSessionNew.getAccount());
                }
                case 0 -> {
                    log.info("Exiting main menu and terminating the application.");
                    return;
                }
                default -> {
                    log.warn("Invalid menu option selected: {}", mode);
                    System.out.println("Invalid option, please try again.");
                }
            }
        }
    }

    private static int choiceTrack() {
        log.debug("Prompting user to enter track number.");
        System.out.println("Enter track number (https://hyperskill.org/tracks): ");
        checkInputNum();
        int trackNumber = Util.getScanner().nextInt();
        log.info("User selected track number: {}", trackNumber);

        return trackNumber;
    }

    private static Account choiceUser() {
        log.debug("Starting user selection process...");
        Settings settings = SettingsManager.loadSettings();
        List<Account> accounts = settings.getAccounts();
        System.out.println("Select a user: ");

        for (int i = 1; i <= accounts.size(); i++) {
            System.out.println(i + ". " + accounts.get(i - 1).login());
        }

        System.out.println("0. Add new user");

        while (true) {
            checkInputNum();
            int userMode = Util.getScanner().nextInt();
            log.debug("User input received: {}", userMode);

            if (userMode <= accounts.size() && userMode != 0) {
                log.info("User selected: {}", accounts.get(userMode - 1).login());
                return accounts.get(userMode - 1);
            } else if (userMode == 0) {
                return addUser();
            } else {
                System.out.println("Select a user from the list!");
                log.warn("Invalid user selection input: {}", userMode);
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
            int dataMode = Util.getScanner().nextInt();

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
        log.info("Starting the process of adding a user...");
        Settings settings = SettingsManager.loadSettings();
        Account account = SettingsManager.getAccount();
        settings.getAccounts().add(account);
        SettingsManager.saveSettings(settings);
        log.info("Account {} successfully added!", account.login());
        System.out.println("Account \"" + account.login() + "\" successfully added!");

        return account;
    }

    private static Account delUser() {
        log.info("Starting the process to delete a user...");
        Settings settings = SettingsManager.loadSettings();
        List<Account> accounts = settings.getAccounts();

        System.out.println("Select a user: ");
        for (int i = 1; i <= accounts.size(); i++) {
            System.out.println(i + ". " + accounts.get(i - 1).login());
        }

        while (true) {
            checkInputNum();
            int userMode = Util.getScanner().nextInt();

            if (userMode <= accounts.size() && userMode != 0) {
                Account deletedAccount = accounts.remove(userMode - 1);
                log.info("Account deleted: {}", deletedAccount.login());
                System.out.println("Account \"" + deletedAccount.login() + "\" deleted!");
                SettingsManager.saveSettings(settings);
                log.debug("Settings saved after deleting an account.");

                return choiceUser();
            } else {
                log.warn("Invalid user selection: {}", userMode);
                System.out.println("Select a user from the list!");
            }
        }
    }

    private static void changePaths() {
        log.info("Entering path change menu.");

        while (true) {
            Settings settings = SettingsManager.loadSettings();

            System.out.printf("""
                            Current paths:
                            driver path: %s
                            folder path: %s
                            """,
                    settings.getChromedriverPath(),
                    settings.getFolderPath());

            System.out.println("""
                    Enter mode number:
                    1. Change driver path
                    2. Change folder path
                    0. Return""");

            checkInputNum();
            int settingsMode = Util.getScanner().nextInt();
            log.debug("Settings mode selected: {}", settingsMode);

            switch (settingsMode) {
                case 1 -> {
                    System.out.println("Enter path ChromeDriver: ");
                    String driverPath = Util.getScanner().next();
                    settings.setChromedriverPath(driverPath);
                    SettingsManager.saveSettings(settings);
                    log.info("ChromeDriver path changed to: {}", driverPath);
                }
                case 2 -> {
                    System.out.println("Enter path FolderPath: ");
                    String folderPath = Util.getScanner().next();
                    settings.setFolderPath(folderPath);
                    SettingsManager.saveSettings(settings);
                    log.info("Folder path changed to: {}", folderPath);
                }
                case 0 -> {
                    log.info("Exiting path change menu.");
                    return;
                }
                default -> {
                    log.warn("Invalid path change option selected: {}", settingsMode);
                    System.out.println("Invalid option, please try again.");
                }
            }
        }
    }

    private static void getDataMenu(UserSession userSession) {
        log.info("Entering Data Menu for user: {}", userSession.getAccount().login());
        DataManager dataManager = new DataManager(userSession);

        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Update data
                    2. Last statistic
                    0. Return""");

            checkInputNum();
            int dataMode = Util.getScanner().nextInt();
            log.debug("Data menu option selected: {}", dataMode);

            switch (dataMode) {
                case 1 -> {
                    try {
                        log.info("Updating data for user: {}", userSession.getAccount().login());
                        System.out.println("The process of retrieving data has started. Please, wait...");

                        WebDriver driver = Util.createDriver(false);
                        Util.login(driver, userSession.getAccount().login(),
                                userSession.getAccount().password());
                        dataManager.getData(driver);
                        dataManager.printStats(1);
                        Util.closeDriver(driver);

                        log.info("Data update completed successfully for user: {}", userSession.getAccount().login());
                        System.out.println("The data has been received successfully!");
                    } catch (Exception e) {
                        System.out.println("Data acquisition error. Please try again.");
                    }
                }
                case 2 -> {
                    System.out.println("Enter the number of statistics to output:");
                    checkInputNum();
                    int lastStats = Util.getScanner().nextInt();
                    log.info("Displaying the latest {} statistics entries.", lastStats);
                    dataManager.printStats(lastStats);
                }
                case 0 -> {
                    log.info("Returning from Data Menu.");
                    return;
                }
                default -> {
                    log.warn("Invalid data menu option selected: {}", dataMode);
                    System.out.println("Invalid option, please try again.");
                }
            }
        }
    }

    private static void savePagesMenu(UserSession userSession) {
        log.info("Entering save pages menu for user: {}", userSession.getAccount().login());

        Settings settings = SettingsManager.loadSettings();
        SavePages save = new SavePages(userSession, settings);

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
            int saveMode = Util.getScanner().nextInt();
            log.debug("Save mode selected: {}", saveMode);

            if (saveMode == 0) {
                log.info("Exiting save pages menu.");
                return;
            }

            try {
                WebDriver driver = Util.createDriver(false);
                log.info("WebDriver created. Starting page loading for saving...");
                System.out.println("The page loading has started. Please wait...");
                Util.login(driver, userSession.getAccount().login(),
                        userSession.getAccount().password());

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
                    default -> {
                        log.warn("Invalid data menu option selected: {}", saveMode);
                        System.out.println("Invalid option, please try again.");
                    }
                }

                log.info("Data saved successfully for mode: {}", saveMode);
                System.out.println("All pages are successfully saved.");
                Util.closeDriver(driver);
            } catch (Exception e) {
                log.error("Error occurred while saving data for mode {}: {}", saveMode, e.getMessage(), e);
            }
        }
    }

    private static void getAnswersMenu(UserSession userSession) {
        log.info("Starting the process to get answers for user: {}", userSession.getAccount().login());

        try {
            WebDriver driver = Util.createDriver(false);
            Automation automation = new Automation(driver, userSession);
            System.out.println("The process of getting answers has started. Please wait...");

            Util.login(driver, userSession.getAccount().login(),
                    userSession.getAccount().password());
            automation.getAnswers();
            log.info("Answers retrieved successfully for user: {}", userSession.getAccount().login());

            Util.closeDriver(driver);
        } catch (Exception e) {
            log.error("An error occurred during the process of getting answers: {}", e.getMessage(), e);
            System.out.println("An error occurred while receiving responses.");
        }
    }

    private static void setAnswersMenu(UserSession userSession) {
        log.info("Initiating the process to send responses for user: {}", userSession.getAccount().login());
        try {
            WebDriver driver = Util.createDriver(true);
            Automation automation = new Automation(driver, userSession);
            System.out.println("The process of sending responses has begun. Please wait...");

            Util.login(driver, userSession.getAccount().login(),
                    userSession.getAccount().password());
            automation.sendAnswers();
            log.info("Responses sent successfully for user: {}", userSession.getAccount().login());

            Util.closeDriver(driver);
        } catch (Exception e) {
            log.error("An error occurred during the process of sending responses: {}", e.getMessage(), e);
            System.out.println("An error occurred while sending responses.");
        }
    }

    private static void checkInputNum() {
        while (!Util.getScanner().hasNextInt()) {
            System.out.println("That's not a number. Please enter a number:");
            Util.getScanner().next();
        }
    }
}