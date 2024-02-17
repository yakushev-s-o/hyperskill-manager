package com.yakushevso;

import com.yakushevso.data.Data;
import com.yakushevso.data.Project;
import com.yakushevso.data.Settings;
import com.yakushevso.data.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SavePages {
    private final String DATA_PATH;
    private final String FOLDER_PATH;
    private final String SITE_LINK = "https://hyperskill.org/";

    public SavePages(Settings settings) {
        DATA_PATH = settings.getDataPath();
        FOLDER_PATH = settings.getFolderPath();
    }

    // Save pages with topics
    public void saveTopics(WebDriver driver) {
        Data data = DataManager.getFileData(Data.class, DATA_PATH);

        for (Integer topic : data.getTopic_relations().getTopics()) {
            if (isFileExists(FOLDER_PATH + "knowledge-map/", String.valueOf(topic))) {
                driver.get(SITE_LINK + "knowledge-map/" + topic);

                Util.waitDownloadElement(driver, "//div[@class='knowledge-map-node']");
                Util.delay(1000);

                if (isVisibleElement(driver, "//div[@class='loader-body card card-body']")) {
                    Util.waitDownloadElement(driver, "//div[@class='topic-block']");
                }

                save(driver, "knowledge-map/", String.valueOf(topic));
            }
        }
    }

    // Save pages with projects
    public void saveProjects(WebDriver driver) {
        Data data = DataManager.getFileData(Data.class, DATA_PATH);

        for (Project project : data.getProjects()) {
            if (isFileExists(FOLDER_PATH + "projects/", String.valueOf(project.getId()))) {
                driver.get(SITE_LINK + "projects/" + project.getId());
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

                save(driver, "projects/", String.valueOf(project.getId()));
            }
        }
    }

    // Save project stages
    public void saveStages(WebDriver driver) {
        Data data = DataManager.getFileData(Data.class, DATA_PATH);

        for (Project project : data.getProjects()) {
            for (String stages : project.getStages_ids()) {
                if (isFileExists(FOLDER_PATH + "projects/" + project.getId() + "/stages/" + stages, "implement")) {
                    driver.get(SITE_LINK + "projects/" + project.getId() + "/stages/" + stages + "/implement");
                    Util.waitDownloadElement(driver, "//div[@class='tabs']");
                    Util.delay(1000);
                    save(driver, "projects/" + project.getId() + "/stages/" + stages + "/", "implement");
                }
            }
        }
    }

    // Save pages with topics
    public void saveThemes(WebDriver driver) {
        Data data = DataManager.getFileData(Data.class, DATA_PATH);

        for (Step steps : data.getSteps()) {
            if (isFileExists(FOLDER_PATH + "learn/step/", String.valueOf(steps.getId()))) {
                driver.get(SITE_LINK + "learn/step/" + steps.getId());
                Util.waitDownloadElement(driver, "//a[@class='text-gray']");

                // Check if the theory is solved and the page is fully expanded
                if (isVisibleElement(driver, "//a[@class='ml-3' and starts-with(normalize-space(text()), 'Expand all')]")) {
                    Actions actions = new Actions(driver);
                    WebElement element = driver.findElement(
                            By.xpath("//a[@class='ml-3' and starts-with(normalize-space(text()), 'Expand all')]"));
                    actions.moveToElement(element).click().perform();
                    Util.waitDownloadElement(driver, "//button[@click-event-target='start_practicing']");
                }

                Util.delay(1000);

                save(driver, "learn/step/", String.valueOf(steps.getId()));
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
