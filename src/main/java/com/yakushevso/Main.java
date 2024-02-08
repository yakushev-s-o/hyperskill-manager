package com.yakushevso;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Enter track number (https://hyperskill.org/tracks):");
        checkInputNum();
        int track = sc.nextInt();
        Util util = new Util(track);

        // Close old drivers
        while (util.isProcessRunning()) {
            util.closeProcess();
        }

        mainMenu(util);
    }

    private static void mainMenu(Util util) {
        while (true) {
            System.out.println("""
                    Enter mode number:
                    1. Get data
                    2. Save pages
                    3. Get answers
                    4. Send answers
                    0. Exit""");

            checkInputNum();
            int mode = sc.nextInt();

            Automation test = new Automation();
            switch (mode) {
                case 1 -> getDataMenu(util);
                case 2 -> savePagesMenu(util);
                case 3 -> getAnswersMenu(util, test);
                case 4 -> sendAnswersMenu(util, test);
                case 0 -> System.exit(0);
                default -> System.out.println("Invalid option, please try again.");
            }
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
                    System.out.println("Enter track:");
                    checkInputNum();
                    int track = sc.nextInt();
                    util.printStats(track);
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

            SavePages save = new SavePages();
            switch (saveMode) {
                case 1 -> {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    save.saveTopics();
                }
                case 2 -> {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    save.saveProjects();
                }
                case 3 -> {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    save.saveStages();
                }
                case 4 -> {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    save.saveThemes();
                }
                case 5 -> {
                    save.saveTopics();
                    save.saveProjects();
                    save.saveStages();
                    save.saveThemes();
                }
                case 0 -> {
                    return;
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

    private static void sendAnswersMenu(Util util, Automation test) {
        util.createDriver(false);
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
}