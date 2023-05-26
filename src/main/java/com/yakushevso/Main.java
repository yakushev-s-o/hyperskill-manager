package com.yakushevso;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Enter track number:");

            int track = sc.nextInt();

            Automation test = new Automation();
            SavePages save = new SavePages();
            Util util = new Util(track);

            while (true) {
                System.out.println("""
                        Select mode:
                        1. Get data
                        2. Save pages
                        3. Get the right answers
                        4. Answer tests
                        5. Exit""");

                int mode = sc.nextInt();

                if (mode == 1) {
                    util.createDriver(true);
                    util.login();
                    util.getData(12);
                } else if (mode == 2) {
                    System.out.println("""
                        Select mode:
                        1. Save topics
                        2. Save projects
                        3. Save stages
                        4. Save themes
                        5. Save all""");

                    int saveMode = sc.nextInt();

                    util.createDriver(false);
                    util.login();

                    if (saveMode == 1) {
                        save.saveTopics();
                    } else if (saveMode == 2) {
                        save.saveProjects();
                    } else if (saveMode == 3) {
                        save.saveStages();
                    } else if (saveMode == 4) {
                        save.saveSteps();
                    } else if (saveMode == 5) {
                        save.saveTopics();
                        save.saveProjects();
                        save.saveStages();
                        save.saveSteps();
                    }
                } else if (mode == 3) {
                    util.createDriver(true);
                    util.login();
                    test.getAnswers();
                } else if (mode == 4) {
                    util.createDriver(false);
                    util.login();
                    test.sendAnswers();
                } else if (mode == 5) {
                    System.exit(0);
                }
            }
        }
    }
}
