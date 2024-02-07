package com.yakushevso;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Enter track number (https://hyperskill.org/tracks):");

            int track = sc.nextInt();

            Automation test = new Automation();
            SavePages save = new SavePages();
            Util util = new Util(track);

            // Close old drivers
            while (util.isProcessRunning()) {
                util.closeProcess();
            }

            while (true) {
                System.out.println("""
                        Enter mode number:
                        1. Get data
                        2. Save pages
                        3. Get answers
                        4. Send answers
                        5. Exit""");

                int mode = sc.nextInt();

                if (mode == 1) {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    util.getData(track);
                } else if (mode == 2) {
                    System.out.println("""
                            Enter mode number:
                            1. Save topics
                            2. Save projects
                            3. Save stages
                            4. Save themes
                            5. Save all""");

                    int saveMode = sc.nextInt();

                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();

                    if (saveMode == 1) {
                        save.saveTopics();
                    } else if (saveMode == 2) {
                        save.saveProjects();
                    } else if (saveMode == 3) {
                        save.saveStages();
                    } else if (saveMode == 4) {
                        save.saveThemes();
                    } else if (saveMode == 5) {
                        save.saveTopics();
                        save.saveProjects();
                        save.saveStages();
                        save.saveThemes();
                    }
                } else if (mode == 3) {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    test.getAnswers();
                } else if (mode == 4) {
                    util.createDriver(false);
                    System.out.println("In progress...");
                    util.login();
                    test.sendAnswers();
                } else if (mode == 5) {
                    System.exit(0);
                }

                System.out.println("Completed!");

                util.closeDriver();
            }
        }
    }
}
