package com.yakushevso;

import com.yakushevso.data.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SavePages {
    private final int TRACK;
    private final String FOLDER_PATH;
    private static final Logger log = LoggerFactory.getLogger(SavePages.class);

    public SavePages(UserSession userSession, Settings settings) {
        TRACK = userSession.getTrack();
        FOLDER_PATH = settings.getFolderPath();
    }

    // Save pages with topics
    public void saveTopics(WebDriver driver) {
        log.info("Starting to save topics for track: {}", TRACK);

        Data data = DataManager.getFileData(Data.class, "src/main/resources/data-list-" + TRACK + ".json");
        if (data == null) {
            log.error("Failed to load data for track: {}. Cannot proceed with saving topics.", TRACK);
            return;
        }


        for (Integer topic : data.topicRelations().topics()) {
            String filePath = FOLDER_PATH + "track/" + TRACK + "/knowledge-map/";
            if (isFileExists(filePath, String.valueOf(topic))) {
                try {
                    log.debug("Loading topic page: {}", topic);
                    driver.get("https://hyperskill.org/knowledge-map/" + topic);

                    Util.waitDownloadElement(driver, "//div[@class='knowledge-map-node']");
                    Util.delay(1000);

                    if (isVisibleElement(driver, "//div[@class='loader-body card card-body']")) {
                        Util.waitDownloadElement(driver, "//div[@class='topic-block']");
                    }

                    save(driver, "/knowledge-map/", String.valueOf(topic));
                    log.info("Topic page saved successfully: {}", topic);
                } catch (Exception e) {
                    log.error("Failed to save topic page for topic {}: {}", topic, e.getMessage(), e);
                }
            } else {
                log.debug("File already exists and will not be downloaded again: {}", filePath + topic);
            }
        }

        log.info("All topics have been successfully saved for track: {}", TRACK);
    }

    // Save pages with projects
    public void saveProjects(WebDriver driver) {
        log.info("Starting to save projects for track: {}", TRACK);

        Data data = DataManager.getFileData(Data.class, "src/main/resources/data-list-" + TRACK + ".json");
        if (data == null) {
            log.error("Failed to load data for track: {}. Cannot proceed with saving projects.", TRACK);
            return;
        }


        for (Project project : data.projects()) {
            String filePath = FOLDER_PATH + "track/" + TRACK + "/projects/";
            if (isFileExists(filePath, String.valueOf(project.id()))) {
                try {
                    log.debug("Loading project page: {}", project.id());
                    driver.get("https://hyperskill.org/projects/" + project.id());
                    Util.waitDownloadElement(driver, "//div[@class='collapse show']");

                    List<WebElement> stageProjectClose = driver.findElements(By.xpath("//button[@class='btn " +
                            "section-collapse-title d-flex align-items-baseline btn-link']//div[@class='icon tw-text-sm']"));

                    Actions actions = new Actions(driver);
                    for (WebElement stage : stageProjectClose) {
                        actions.moveToElement(stage).click().perform();
                        Util.delay(1000);

                        // Check for the throbber element
                        while (isVisibleElement(driver, "//div[@class='collapse show']//div[@class='loader-body']")) {
                            Util.delay(1000);
                        }
                    }

                    save(driver, "/projects/", String.valueOf(project.id()));
                    log.info("Project page saved successfully: {}", project.id());
                } catch (Exception e) {
                    log.error("Failed to save project page for project {}: {}", project.id(), e.getMessage(), e);
                }
            } else {
                log.debug("Project file already exists and will not be downloaded again: {}", filePath + project.id());
            }
        }

        log.info("All projects have been successfully saved for track: {}", TRACK);
    }

    // Save project stages
    public void saveStages(WebDriver driver) {
        log.info("Starting to save stages for track: {}", TRACK);

        Data data = DataManager.getFileData(Data.class, "src/main/resources/data-list-" + TRACK + ".json");
        if (data == null) {
            log.error("Failed to load data for track: {}. Cannot proceed with saving stages.", TRACK);
            return;
        }

        for (Project project : data.projects()) {
            for (String stage : project.stagesIds()) {
                String filePath = FOLDER_PATH + "track/" + TRACK + "/projects/" + project.id() + "/stages/" + stage;
                if (isFileExists(filePath, "implement")) {
                    try {
                        log.debug("Loading stage page: {} for project: {}", stage, project.id());
                        driver.get("https://hyperskill.org/projects/" + project.id()
                                + "/stages/" + stage + "/implement");
                        Util.waitDownloadElement(driver, "//div[@class='tabs']");
                        Util.delay(1000);

                        save(driver, "/projects/" + project.id() + "/stages/" + stage + "/", "implement");
                        log.info("Stage page saved successfully: {} for project: {}", stage, project.id());
                    } catch (Exception e) {
                        log.error("Failed to save stage page for stage: {} of project {}: {}", stage, project.id(), e.getMessage(), e);
                    }
                } else {
                    log.debug("Stage file already exists and will not be downloaded again: {}", filePath + "implement");
                }
            }
        }

        log.info("All stages have been successfully saved for track: {}", TRACK);
    }

    // Save pages with topics
    public void saveThemes(WebDriver driver) {
        log.info("Starting to save themes for track: {}", TRACK);
        Data data = DataManager.getFileData(Data.class, "src/main/resources/data-list-" + TRACK + ".json");
        if (data == null) {
            log.error("Failed to load data for track: {}. Cannot proceed with saving themes.", TRACK);
            return;
        }

        for (Step step : data.steps()) {
            String filePath = FOLDER_PATH + "track/" + TRACK + "/learn/step/";
            if (isFileExists(filePath, String.valueOf(step.id()))) {
                try {
                    log.debug("Loading theme page: {}", step.id());
                    driver.get("https://hyperskill.org/learn/step/" + step.id());
                    Util.waitDownloadElement(driver, "//a[@class='text-gray']");

                    // Check if the theory is solved and the page is fully expanded
                    String expandAll = "//a[@class='ml-3' and starts-with(normalize-space(text()), 'Expand all')]";
                    if (isVisibleElement(driver, expandAll)) {
                        WebElement element = driver.findElement(By.xpath(expandAll));
                        new Actions(driver).moveToElement(element).click().perform();
                        log.debug("Expanded all sections on theme page: {}", step.id());
                        Util.waitDownloadElement(driver, "//button[@click-event-target='start_practicing']");
                    }

                    Util.delay(1000);
                    save(driver, "/learn/step/", String.valueOf(step.id()));
                    log.info("Theme page saved successfully: {}", step.id());
                } catch (Exception e) {
                    log.error("Failed to save theme page for step {}: {}", step.id(), e.getMessage(), e);
                }
            } else {
                log.debug("Theme file already exists and will not be downloaded again: {}", filePath + step.id());
            }
        }

        log.info("All themes have been successfully saved for track: {}", TRACK);
    }

    // Check if the file exists
    private boolean isFileExists(String path, String fileName) {
        // Creating a Path object representing the path to the file
        Path filePath = Paths.get(path, fileName + ".html");

        // Using the File.exists() method to check the existence of the file
        return !Files.exists(filePath);
    }

    // Check for element visibility
    private boolean isVisibleElement(WebDriver driver, String element) {
        try {
            driver.findElement(By.xpath(element));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Saving pages
    private void save(WebDriver driver, String path, String page) {
        File file = new File(FOLDER_PATH + "track/" + TRACK + path);

        // Check if the folder exists and create it if necessary
        if (!file.exists()) {
            boolean created = file.mkdirs();
            if (!created) {
                return;
            }
        }

        // Get the HTML and CSS code of the page
        String pageSource = (String) ((JavascriptExecutor) driver).executeScript(
                "var html = new XMLSerializer().serializeToString(document.doctype) " +
                        "+ document.documentElement.outerHTML;" +
                        "var css = Array.from(document.styleSheets).reduce((cssCode, styleSheet) => {" +
                        "   try {" +
                        "       Array.from(styleSheet.cssRules).forEach(rule => {" +
                        "           cssCode += rule.cssText + '\\n';" +
                        "       });" +
                        "   } catch(error) {" +
                        "       console.warn('Failed to read CSS rules:', error);" +
                        "   }" +
                        "   return cssCode;" +
                        "}, '');" +
                        "return html + '\\n\\n<style>\\n' + css + '</style>';"
        );

        // Saving the page code to a file and saving the encoding
        try {
            String filePath = FOLDER_PATH + "track/" + TRACK + path + page + ".html";
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
                writer.write(pageSource);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
