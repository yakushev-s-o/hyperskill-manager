package com.yakushevso;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        // Close old drivers
        while (isProcessRunning()) {
            closeProcess();
        }

        mainMenu();
    }

    private static void mainMenu() {
        System.out.println("Enter track number (https://hyperskill.org/tracks):");
        checkInputNum();
        int track = sc.nextInt();
        Util util = new Util(track);
        Automation test = new Automation();

        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Get data
                    2. Save pages
                    3. Get answers
                    4. Send answers
                    5. Change track
                    0. Exit""");

            checkInputNum();
            int mode = sc.nextInt();

            switch (mode) {
                case 1 -> getDataMenu(util);
                case 2 -> savePagesMenu(util);
                case 3 -> getAnswersMenu(util, test);
                case 4 -> setAnswersMenu(util, test);
                case 5 -> mainMenu();
                case 0 -> System.exit(0);
                default -> System.out.println("Invalid option, please try again.");
            }

            System.out.println("Completed!");
        }
    }

    private static void getDataMenu(Util util) {
        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Update data
                    2. Get last statistic
                    0. Return""");

            checkInputNum();
            int dataMode = sc.nextInt();

            switch (dataMode) {
                case 1 -> {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    util.getData();
                    util.printStats();
                    util.closeDriver();
                }
                case 2 -> {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    util.printStats();
                }
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    private static void savePagesMenu(Util util) {
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
            int saveMode = sc.nextInt();

            if (saveMode == 0) {
                return;
            }

            util.createDriver(false);
            System.out.println("In progress...");
            util.login();

            SavePages save = new SavePages();
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

            util.closeDriver();
        }
    }

    private static void getAnswersMenu(Util util, Automation test) {
        util.createDriver(false);
        System.out.println("In progress...");
        util.login();
        test.getAnswers();
        util.closeDriver();
    }

    private static void setAnswersMenu(Util util, Automation test) {
        util.createDriver(true);
        System.out.println("In progress...");
        util.login();
        test.sendAnswers();
        util.closeDriver();
    }

    private static void checkInputNum() {
        while (!sc.hasNextInt()) {
            System.out.println("That's not a number. Please enter a number:");
            sc.next();
        }
    }

    // Close process
    private static void closeProcess() {
        try {
            ProcessBuilder builder = new ProcessBuilder("taskkill", "/F", "/IM", "chromedriver.exe");
            builder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Check running process
    private static boolean isProcessRunning() {
        try {
            ProcessBuilder builder = new ProcessBuilder("tasklist");
            Process process = builder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("chromedriver.exe")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}