package com.yakushevso;

import com.google.gson.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;

import com.yakushevso.data.Data;
import com.yakushevso.data.Step;
import com.yakushevso.data.Project;
import com.yakushevso.data.Topic;

public class Util {
    public static WebDriver driver;
    private static String LOGIN;
    private static String PASSWORD;
    private static String CHROMEDRIVER_PATH;
    public static String SITE_LINK;
    public static String FOLDER_PATH;
    public static String JSON_PATH;
    public static String DATA_PATH;

    public Util(int track) {
        initSettings();
        setSettings(track);
    }

    public void createDriver(boolean visible) {
        // Set path to browser driver
        System.setProperty("webdriver.chrome.driver", CHROMEDRIVER_PATH);
        ChromeOptions options = new ChromeOptions();

        // Create an instance of the driver in the background if "true"
        if (visible) {
            options.addArguments("--start-maximized");
        } else {
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
        }

        driver = new ChromeDriver(options);
    }

    // Perform authorization on the site
    public void login() {
        driver.get("https://hyperskill.org/login");

        waitDownloadElement("//input[@type='email']");

        WebElement emailField = driver.findElement(By.xpath("//input[@type='email']"));
        WebElement passwordField = driver.findElement(By.xpath("//input[@type='password']"));
        WebElement signInButton = driver.findElement(By.xpath("//button[@data-cy='submitButton']"));

        emailField.sendKeys(LOGIN);
        passwordField.sendKeys(PASSWORD);
        signInButton.click();

        waitDownloadElement("//h1[@data-cy='curriculum-header']");

        // Close banner "Save streak"
        closeBanner("//button[@class='btn btn-outline-dark' and text()= 'No, thanks']");

        // Close banner "You probably already know this topic"
        closeBanner("//button[@class='btn btn-outline-dark' and text()= 'Continue with theory']");
    }

    // Get track data and write to file
    public void getData(int track) {
        Topic topic = getTopics(track);
        List<Project> projects = getProjects(track);
        List<Step> steps = getSteps(topic);
        List<Step> stepsAdditional = getStepsAdditional(topic, steps);


        try (FileWriter writer = new FileWriter(DATA_PATH)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(new Data(topic, projects, steps, stepsAdditional), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        printProgress(topic, projects, steps, stepsAdditional, track);
    }

    // Print statistics of the received data
    private void printProgress(Topic topic, List<Project> projects, List<Step> steps,
                               List<Step> stepsAdditional, int track) {
        String urlProgress = "https://hyperskill.org/api/progresses/track-" + track + "?format=json";

        driver.get(urlProgress);

        // Get page content as text
        String json = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement progressElement = JsonParser.parseString(json);
        JsonObject progressObj = progressElement.getAsJsonObject()
                .getAsJsonArray("progresses").get(0).getAsJsonObject();

        int stepsSolved = steps.stream().mapToInt(countStep -> countStep.getStepListTrue().size()).sum();
        int stepsUnresolved = steps.stream().mapToInt(countStep -> countStep.getStepListFalse().size()).sum();
        int stepsAdditionalSolved = stepsAdditional.stream().mapToInt(countStep -> countStep.getStepListTrue().size()).sum();
        int stepsAdditionalUnresolved = stepsAdditional.stream().mapToInt(countStep -> countStep.getStepListFalse().size()).sum();

        System.out.printf("""
                        =================================
                        Topics:                      %d
                        Projects:                    %d
                        Completed projects:          %d
                        Theory:                      %d
                        Learned theory:              %d
                        Skipped theory:              %d
                        Steps:                       %d
                        Solved steps:                %d
                        Unresolved steps:            %d
                        Additional Theory:           %d
                        Additional Steps:            %d
                        Additional Solved steps:     %d
                        Additional Unresolved steps: %d
                        =================================""",
                topic.getTopics().size(),
                projects.size(),
                progressObj.getAsJsonArray("completed_projects").size(),
                steps.size(),
                progressObj.get("learned_topics_count").getAsInt(),
                progressObj.get("skipped_topics_count").getAsInt(),
                stepsSolved + stepsUnresolved,
                stepsSolved,
                stepsUnresolved,
                stepsAdditional.size(),
                stepsAdditionalSolved + stepsAdditionalUnresolved,
                stepsAdditionalSolved,
                stepsAdditionalUnresolved);
    }

    // Get the list of topics
    private Topic getTopics(int track) {
        List<String> listTopic = new ArrayList<>();
        List<String> listDescendants = new ArrayList<>();

        int i = 1;
        boolean isNext = true;

        // While there is a next page, we loop
        while (isNext) {
            String urlTopics = "https://hyperskill.org/api/topic-relations?format=json&track_id="
                    + track + "&page_size=100&page=" + i++ + "";

            driver.get(urlTopics);

            // Get page content as text
            String json = driver.findElement(By.tagName("pre")).getText();

            // Get JSON object from text
            JsonElement topicsElement = JsonParser.parseString(json);
            JsonObject topicsObject = topicsElement.getAsJsonObject();

            // Check if there is a next data page
            if (!topicsObject.getAsJsonObject("meta").get("has_next").getAsBoolean()) {
                isNext = false;
            }

            // Get an array of topics
            JsonArray topicRelationsArr = topicsObject.getAsJsonArray("topic-relations");

            for (JsonElement element : topicRelationsArr) {
                JsonObject obj = element.getAsJsonObject();
                listTopic.add(String.valueOf(obj.get("id")));

                // Check if the topic is a parent
                if (obj.get("parent_id").isJsonNull()) {
                    JsonArray descendantsArr = obj.getAsJsonArray("descendants");

                    // Get an array of child topics
                    for (JsonElement s : descendantsArr) {
                        listDescendants.add(String.valueOf(s));
                    }
                }
            }
        }

        return new Topic(listTopic, listDescendants);
    }

    // Get a list of projects
    public List<Project> getProjects(int track) {
        List<Project> projectList = new ArrayList<>();

        String urlTrack = "https://hyperskill.org/api/tracks/" + track + "?format=json";

        driver.get(urlTrack);

        // Get page content as text
        String json = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement trackElement = JsonParser.parseString(json);
        JsonObject trackObj = trackElement.getAsJsonObject()
                .getAsJsonArray("tracks").get(0).getAsJsonObject();

        // Get an object of projects_by_level
        JsonObject projectsByLevel = trackObj.getAsJsonObject("projects_by_level");

        // Iterate over all keys in projects_by_level
        for (Map.Entry<String, JsonElement> entry : projectsByLevel.entrySet()) {
            JsonArray projects = entry.getValue().getAsJsonArray();

            // Getting data from projects
            for (JsonElement projectName : projects) {
                String urlProject = "https://hyperskill.org/api/projects/"
                        + projectName.getAsInt() + "?format=json";
                driver.get(urlProject);
                String projectPageSource = driver.getPageSource();
                String projectJson = projectPageSource.substring(projectPageSource.indexOf("{"),
                        projectPageSource.lastIndexOf("}") + 1);

                JsonElement projectJsonElement = JsonParser.parseString(projectJson);
                JsonObject projectJsonObject = projectJsonElement.getAsJsonObject();
                JsonArray projectDataArray = projectJsonObject.getAsJsonArray("projects");

                for (JsonElement projectElement1 : projectDataArray) {
                    JsonObject projectObj1 = projectElement1.getAsJsonObject();

                    int id = projectObj1.get("id").getAsInt();
                    String title = projectObj1.get("title").getAsString();
                    List<String> stagesIds = new ArrayList<>();

                    for (JsonElement stageId : projectObj1.getAsJsonArray("stages_ids")) {
                        stagesIds.add(stageId.getAsString());
                    }

                    projectList.add(new Project(id, SITE_LINK + "projects/" + id,
                            title, stagesIds));
                }
            }
        }

        return projectList;
    }

    // Get a list of topics and tasks
    private List<Step> getSteps(Topic topics) {
        List<Step> steps = new ArrayList<>();

        for (String topic : topics.getDescendants()) {
            int i = 1;
            boolean isNext = true;

            // While there is a next page, we loop
            while (isNext) {
                String url = "https://hyperskill.org/api/steps?format=json&topic=" + topic +
                        "&page_size=100&page=" + i++ + "";

                driver.get(url);

                // Get page content as text
                String pageSource = driver.findElement(By.tagName("pre")).getText();

                // Get JSON object with data
                JsonElement jsonElement = JsonParser.parseString(pageSource);
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                // Check if there is a next data page
                if (!jsonObject.getAsJsonObject("meta").get("has_next").getAsBoolean()) {
                    isNext = false;
                }

                int id = 0;
                String title = "";
                List<String> listStepTrue = new ArrayList<>();
                List<String> listStepFalse = new ArrayList<>();

                // Get an array of steps
                JsonArray topicRelationsArr = jsonObject.getAsJsonArray("steps");

                for (JsonElement element : topicRelationsArr) {
                    JsonObject obj = element.getAsJsonObject();

                    // Check the step type (theory or practice)
                    if (obj.get("type").getAsString().equals("theory")) {
                        // If the type is a theory, then get the theory ID and name
                        id = obj.get("topic_theory").getAsInt();
                        title = obj.get("title").getAsString();
                    } else if (obj.get("type").getAsString().equals("practice")) {
                        // Divide the lists into completed and uncompleted
                        if (obj.get("is_completed").getAsBoolean()) {
                            // If "practice", then add practice ID
                            listStepTrue.add(obj.get("id").getAsString());
                        } else {
                            listStepFalse.add(obj.get("id").getAsString());
                        }
                    }
                }

                steps.add(new Step(id, Integer.parseInt(topic),
                        SITE_LINK + "learn/step/" + id, title, listStepTrue, listStepFalse));
            }
        }

        return steps;
    }

    // Get a list of topics and tasks outside track
    private List<Step> getStepsAdditional(Topic topics, List<Step> step) {
        List<Step> stepsAdditional = new ArrayList<>();
        Set<Integer> followerList = new HashSet<>();

        for (String topic : topics.getDescendants()) {
            String urlFollowers = "https://hyperskill.org/api/topics?format=json&ids=" + topic;

            driver.get(urlFollowers);

            // Get page content as text
            String pageSourceFollowers = driver.findElement(By.tagName("pre")).getText();

            // Get JSON object with data
            JsonElement jsonElementFollowers = JsonParser.parseString(pageSourceFollowers);
            JsonObject jsonObjFollowers = jsonElementFollowers.getAsJsonObject();

            // Get array followers
            JsonObject topicObjFollowers = jsonObjFollowers.getAsJsonArray("topics").get(0).getAsJsonObject();
            JsonArray followers = topicObjFollowers.getAsJsonArray("followers");

            // Removing duplicates
            for (JsonElement checkFollowers : followers) {
                followerList.add(checkFollowers.getAsInt());
            }
        }

        for (Integer checkFollowers : followerList) {
            boolean check = true;
            for (Step checkStep : step) {
                if (checkFollowers.equals(checkStep.getId())) {
                    check = false;
                    break;
                }
            }

            if (check) {
                int i = 1;
                boolean isNext = true;

                // While there is a next page, we loop
                while (isNext) {
                    String url2 = "https://hyperskill.org/api/steps?format=json&topic=" + checkFollowers +
                            "&page_size=100&page=" + i++ + "";

                    driver.get(url2);

                    // Get page content as text
                    String pageSource2 = driver.findElement(By.tagName("pre")).getText();

                    // Get JSON object
                    JsonElement jsonElement2 = JsonParser.parseString(pageSource2);
                    JsonObject jsonObject2 = jsonElement2.getAsJsonObject();

                    // Check if there is a next data page
                    if (!jsonObject2.getAsJsonObject("meta").get("has_next").getAsBoolean()) {
                        isNext = false;
                    }

                    int id = 0;
                    String title = "";
                    List<String> listStepTrue = new ArrayList<>();
                    List<String> listStepFalse = new ArrayList<>();

                    // Get an array of steps
                    JsonArray topicRelationsArr = jsonObject2.getAsJsonArray("steps");

                    for (JsonElement element : topicRelationsArr) {
                        JsonObject obj = element.getAsJsonObject();

                        // Check the step type (theory or practice)
                        if (obj.get("type").getAsString().equals("theory")) {
                            // If the type is a theory, then get the theory ID and name
                            id = obj.get("topic_theory").getAsInt();
                            title = obj.get("title").getAsString();
                        } else if (obj.get("type").getAsString().equals("practice")) {
                            // Divide the lists into completed and uncompleted
                            if (obj.get("is_completed").getAsBoolean()) {
                                // If "practice", then add practice ID
                                listStepTrue.add(obj.get("id").getAsString());
                            } else {
                                listStepFalse.add(obj.get("id").getAsString());
                            }
                        }
                    }

                    stepsAdditional.add(new Step(id, checkFollowers,
                            SITE_LINK + "learn/step/" + id, title, listStepTrue, listStepFalse));
                }
            }
        }

        return stepsAdditional;
    }

    // Get a list of objects from a file
    public static <T> T getFileData(Type type, String path) {
        Gson gson = new Gson();
        File file = new File(path);
        T result = null;

        if (file.exists() && file.length() != 0) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                JsonElement jsonElement = gson.fromJson(reader, JsonElement.class);

                if (jsonElement.isJsonArray()) {
                    // Read the list of objects
                    result = gson.fromJson(jsonElement, type);
                } else {
                    // Read single object
                    result = gson.fromJson(jsonElement.getAsJsonObject(), type);
                }

                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    // Save the object to a JSON file
    public static <T> void saveToFile(T answer, List<T> list, String path) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path);

        // Append new data to existing ones in memory
        list.add(answer);

        // Write updated data to file
        try {
            FileWriter writer = new FileWriter(file);
            gson.toJson(list, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Check if the element has loaded
    public static boolean waitDownloadElement(String xpath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        return wait.until(ExpectedConditions.and(
                ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)),
                ExpectedConditions.elementToBeClickable(By.xpath(xpath))
        ));
    }

    // Delay between transitions
    public static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Close process
    public void closeProcess() {
        try {
            ProcessBuilder builder = new ProcessBuilder("taskkill", "/F", "/IM", "chromedriver.exe");
            builder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Check running process
    public boolean isProcessRunning() {
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

    // Create settings file
    private void initSettings() {
        File file = new File("src/main/resources/settings.json");

        if (!file.exists() || file.length() == 0) {
            LinkedHashMap<String, String> settingsData = new LinkedHashMap<>();

            settingsData.put("login", "YOUR_LOGIN");
            settingsData.put("password", "YOUR_PASSWORD");
            settingsData.put("chromedriver_path", "C:/tools/chromedriver_win32/chromedriver.exe");
            settingsData.put("folder_path", "C:/Users/Admin/Desktop/track/TRACK_NUMBER/");
            settingsData.put("json_path", "src/main/resources/answer-list-TRACK_NUMBER.json");
            settingsData.put("data_path", "src/main/resources/data-list-TRACK_NUMBER.json");
            settingsData.put("site_link", "https://hyperskill.org/");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            try {
                FileWriter writer = new FileWriter(file);
                gson.toJson(settingsData, writer);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Set settings
    private void setSettings(int track) {
        Gson gson = new Gson();
        File file = new File("src/main/resources/settings.json");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            JsonElement jsonElement = gson.fromJson(reader, JsonElement.class);
            JsonObject obj = jsonElement.getAsJsonObject();

            LOGIN = obj.get("login").getAsString();
            PASSWORD = obj.get("password").getAsString();
            CHROMEDRIVER_PATH = obj.get("chromedriver_path").getAsString();
            FOLDER_PATH = obj.get("folder_path").getAsString().replace("TRACK_NUMBER", String.valueOf(track));
            JSON_PATH = obj.get("json_path").getAsString().replace("TRACK_NUMBER", String.valueOf(track));
            DATA_PATH = obj.get("data_path").getAsString().replace("TRACK_NUMBER", String.valueOf(track));
            SITE_LINK = obj.get("site_link").getAsString();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Close ChromeDriver
    public void closeDriver() {
        driver.quit();
    }

    // Close drop-down banner
    private void closeBanner(String element) {
        try {
            delay(5000);
            WebElement banner = driver.findElement(By.xpath(element));
            Actions actions = new Actions(driver);
            actions.moveToElement(banner).click().perform();
        } catch (Exception ignored) {
        }
    }
}