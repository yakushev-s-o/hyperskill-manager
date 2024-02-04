package com.yakushevso;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.yakushevso.data.Data;
import com.yakushevso.data.Step;
import com.yakushevso.data.Project;

import static com.yakushevso.Util.*;

public class SavePages {
    // Save pages with topics
    public void saveTopics() {
        Data data = getFileData(Data.class, DATA_PATH);

        for (String topic : data.getTopic_relations().getTopics()) {
            if (isFileExists(FOLDER_PATH + "knowledge-map/", topic)) {
                driver.get(SITE_LINK + "knowledge-map/" + topic);
                waitDownloadElement("//div[@class='knowledge-map-node']");
                delay(1000);
                save("knowledge-map/", topic);
            }
        }
    }

    // Save pages with projects
    public void saveProjects() {
        Data data = getFileData(Data.class, DATA_PATH);

        for (Project project : data.getProjects()) {
            if (isFileExists(FOLDER_PATH + "projects/", String.valueOf(project.getId()))) {
                driver.get(SITE_LINK + "projects/" + project.getId());
                waitDownloadElement("//button[@class='btn section-collapse-title d-flex align-items-baseline btn-link']");

                List<WebElement> stageProjectClose = driver.findElements(By.xpath("//button[@class='btn " +
                        "section-collapse-title d-flex align-items-baseline btn-link']//div[@class='icon tw-text-sm']"));

                Actions actions = new Actions(driver);
                while (stageProjectClose.size() > 0) {
                    for (WebElement stage : stageProjectClose) {
                        actions.moveToElement(stage).click().perform();
                        delay(1000);
                    }

                    stageProjectClose = driver.findElements(By.xpath("//button[@class='btn " +
                            "section-collapse-title d-flex align-items-baseline btn-link']//div[@class='icon tw-text-sm']"));
                }

                delay(1000);

                save("projects/", String.valueOf(project.getId()));
            }
        }
    }

    // Save project milestones
    public void saveStages() {
        Data data = getFileData(Data.class, DATA_PATH);

        for (Project project : data.getProjects()) {
            for (String stages : project.getStages_ids()) {
                if (isFileExists(FOLDER_PATH + "projects/" + project.getId() + "/stages/" + stages, "implement")) {
                    driver.get(SITE_LINK + "projects/" + project.getId() + "/stages/" + stages + "/implement");
                    waitDownloadElement("//div[@class='tabs']");
                    delay(1000);
                    save("projects/" + project.getId() + "/stages/" + stages + "/", "implement");
                }
            }
        }
    }

    // Save pages with topics
    public void saveThemes() {
        Data data = getFileData(Data.class, DATA_PATH);

        for (Step steps : data.getSteps()) {
            if (isFileExists(FOLDER_PATH + "learn/step/", String.valueOf(steps.getId()))) {
                driver.get(SITE_LINK + "learn/step/" + steps.getId());
                waitDownloadElement("//a[@class='text-gray']");

                // Check if the theory is solved
                if (isHideTheory()) {
                    Actions actions = new Actions(driver);
                    WebElement element = driver.findElement(
                            By.xpath("//a[@class='ml-3' and starts-with(normalize-space(text()), 'Expand all')]"));
                    actions.moveToElement(element).click().perform();
                    waitDownloadElement("//button[@click-event-target='start_practicing']");
                }

                delay(1000);

                save("learn/step/", String.valueOf(steps.getId()));
            }
        }
    }

    // Check if the file exists
    private boolean isFileExists(String path, String fileName) {
        File dir = new File(path);
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(fileName + ".html")) {
                    return false;
                }
            }
        }

        return true;
    }

    // Check if the page is fully expanded
    private boolean isHideTheory() {
        try {
            driver.findElement(By.xpath("//a[@class='ml-3' and starts-with(normalize-space(text()), 'Expand all')]"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Saving pages
    private void save(String path, String page) {
        File file = new File(FOLDER_PATH + path);

        // Check if the folder exists and create it if necessary
        if (!file.exists()) {
            boolean created = file.mkdirs();
            if (!created) {
                return;
            }
        }

        // Get the HTML and CSS code of the page
        String pageSource = (String) ((JavascriptExecutor) driver).executeScript(
                "var html = new XMLSerializer().serializeToString(document.doctype) + document.documentElement.outerHTML;" +
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
            String filePath = FOLDER_PATH + path + page + ".html";
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));
            writer.write(pageSource);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
