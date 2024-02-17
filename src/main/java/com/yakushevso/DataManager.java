package com.yakushevso;

import com.google.gson.*;
import com.yakushevso.data.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class DataManager {
    private final int TRACK;
    private final String DATA_PATH;
    private final String SITE_LINK = "https://hyperskill.org/";

    public DataManager(UserSession userSession, Settings settings) {
        TRACK = userSession.getTrack();
        DATA_PATH = settings.getDataPath();
    }

    // Get track data and write to file
    public void getData(WebDriver driver) {
        Topic topic = getTopics(driver);
        List<Project> projects = getProjects(driver);
        List<Step> steps = getSteps(driver, topic);
        List<Step> additionalSteps = getSteps(driver, getAdditionalTopics(driver, topic, steps));

        try (FileWriter writer = new FileWriter(DATA_PATH)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(new Data(topic, projects, steps, additionalSteps), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get the list of topics
    private Topic getTopics(WebDriver driver) {
        List<Integer> listTopic = new ArrayList<>();
        List<Integer> listDescendants = new ArrayList<>();

        int i = 1;
        boolean isNext = true;

        // While there is a next page, we loop
        while (isNext) {
            String urlTopics = "https://hyperskill.org/api/topic-relations?format=json&track_id="
                    + TRACK + "&page_size=100&page=" + i++ + "";

            driver.get(urlTopics);

            // Get page content as text
            String json = driver.findElement(By.tagName("pre")).getText();

            // Get JSON object from text
            JsonElement topicsElement = JsonParser.parseString(json);
            JsonObject topicsObject = topicsElement.getAsJsonObject();

            // Get an array of topics
            JsonArray topicRelationsArr = topicsObject.getAsJsonArray("topic-relations");

            for (JsonElement element : topicRelationsArr) {
                JsonObject obj = element.getAsJsonObject();
                listTopic.add(obj.get("id").getAsInt());

                // Check if the topic is a parent
                if (obj.get("parent_id").isJsonNull()) {
                    JsonArray descendantsArr = obj.getAsJsonArray("descendants");

                    // Get an array of child topics
                    for (JsonElement s : descendantsArr) {
                        listDescendants.add(s.getAsInt());
                    }
                }
            }

            // Check if there is a next data page
            if (!topicsObject.getAsJsonObject("meta").get("has_next").getAsBoolean()) {
                isNext = false;
            }
        }

        return new Topic(listTopic, listDescendants);
    }

    // Get a list of projects
    public List<Project> getProjects(WebDriver driver) {
        List<Project> projectList = new ArrayList<>();

        String urlTrack = "https://hyperskill.org/api/tracks/" + TRACK + "?format=json";

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
            for (JsonElement project : projects) {
                String urlProject = "https://hyperskill.org/api/projects/"
                        + project.getAsInt() + "?format=json";

                driver.get(urlProject);

                // Get page content as text
                String jsonProject = driver.findElement(By.tagName("pre")).getText();

                // Get JSON object from text
                JsonElement projectElement = JsonParser.parseString(jsonProject);
                JsonObject projectObj = projectElement.getAsJsonObject()
                        .getAsJsonArray("projects").get(0).getAsJsonObject();

                int id = projectObj.get("id").getAsInt();
                String title = projectObj.get("title").getAsString();
                List<String> stagesIds = new ArrayList<>();

                for (JsonElement stageId : projectObj.getAsJsonArray("stages_ids")) {
                    stagesIds.add(stageId.getAsString());
                }

                JsonObject progressObj = getProgress(driver, "https://hyperskill.org/api/progresses" +
                        "?format=json&ids=project-" + project);
                boolean completed = progressObj.get("is_completed").getAsBoolean();

                projectList.add(new Project(id, completed, SITE_LINK + "projects/" + id,
                        title, stagesIds));
            }
        }

        return projectList;
    }

    // Get a list of topics and tasks
    private List<Step> getSteps(WebDriver driver, Topic topics) {
        List<Step> listSteps = new ArrayList<>();

        for (Integer topic : topics.getDescendants()) {
            JsonArray listStep = new JsonArray();
            boolean isNext = true;
            int pageNum = 1;

            // While there is a next page, we loop
            while (isNext) {
                String url = "https://hyperskill.org/api/steps?format=json&topic=" + topic +
                        "&page_size=100&page=" + pageNum++ + "";

                driver.get(url);

                // Get page content as text
                String pageSource = driver.findElement(By.tagName("pre")).getText();

                // Get JSON object with data
                JsonElement jsonElement = JsonParser.parseString(pageSource);
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                // Add a step to the array list
                for (JsonElement element : jsonObject.getAsJsonArray("steps")) {
                    listStep.add(element);
                }

                // Check if there is a next data page
                if (!jsonObject.getAsJsonObject("meta").get("has_next").getAsBoolean()) {
                    isNext = false;
                }
            }

            int theory = 0;
            String titleTheory = "";
            boolean learnedTheory = false;
            List<String> listStepTrue = new ArrayList<>();
            List<String> listStepFalse = new ArrayList<>();

            // Get detailed information on the step
            for (JsonElement step : listStep) {
                JsonObject objStep = step.getAsJsonObject();

                // Check the step type (theory or practice)
                if (objStep.get("type").getAsString().equals("theory")) {
                    // If the type is a theory, then get the theory ID and name
                    theory = objStep.get("topic_theory").getAsInt();
                    titleTheory = objStep.get("title").getAsString();
                    learnedTheory = objStep.get("is_completed").getAsBoolean();
                } else if (objStep.get("type").getAsString().equals("practice")) {
                    // Divide the lists into completed and uncompleted
                    if (objStep.get("is_completed").getAsBoolean()) {
                        // If "practice", then add practice ID
                        listStepTrue.add(objStep.get("id").getAsString());
                    } else {
                        listStepFalse.add(objStep.get("id").getAsString());
                    }
                }
            }

            JsonObject progressObj = getProgress(driver, "https://hyperskill.org/api/progresses" +
                    "?format=json&ids=topic-" + topic);

            // Progress of the topic
            float capacityTopic = progressObj.get("capacity").getAsFloat();
            boolean learnedTopic = progressObj.get("is_learned").getAsBoolean();
            boolean skippedTopic = progressObj.get("is_skipped").getAsBoolean();

            listSteps.add(new Step(theory, topic, learnedTopic, skippedTopic, capacityTopic,
                    SITE_LINK + "learn/step/" + theory, titleTheory,
                    learnedTheory, listStepTrue, listStepFalse));

        }

        return listSteps;
    }

    // Get a list of topics and tasks outside track
    private Topic getAdditionalTopics(WebDriver driver, Topic topics, List<Step> steps) {
        Set<Integer> followerList = new HashSet<>();
        List<Integer> additionalTopic = new ArrayList<>();

        for (Integer topic : topics.getDescendants()) {
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

        // Formatting the Topics in HashSet
        Set<Integer> topicSet = new HashSet<>(topics.getTopics());

        // Formatting the Steps in HashSet
        Set<Integer> stepSet = new HashSet<>();
        for (Step step : steps) {
            stepSet.add(step.getTopic());
        }

        // Removing duplicates
        for (Integer follower : followerList) {
            if (!topicSet.contains(follower) && !stepSet.contains(follower)) {
                additionalTopic.add(follower);
            }
        }

        return new Topic(null, additionalTopic);
    }

    // Get statistics of the received data
    private String getStatistics(WebDriver driver, Topic topic, List<Project> projects, List<Step> steps,
                                 List<Step> additionalSteps) {
        JsonObject currentObj = getCurrent(driver).getAsJsonObject("gamification");
        JsonObject progressObj = getProgress(driver, "https://hyperskill.org/api/progresses"
                + "/track-" + TRACK + "?format=json");

        int stepsSolved = steps.stream().mapToInt(countStep -> countStep
                .getStepListTrue().size()).sum();
        int stepsUnresolved = steps.stream().mapToInt(countStep -> countStep
                .getStepListFalse().size()).sum();
        int stepsAdditionalSolved = additionalSteps.stream().mapToInt(countStep -> countStep
                .getStepListTrue().size()).sum();
        int stepsAdditionalUnresolved = additionalSteps.stream().mapToInt(countStep -> countStep
                .getStepListFalse().size()).sum();
        long completedTheory = steps.stream().filter(Step::learned_theory).count();
        long completedAdditionalTopics = additionalSteps.stream().filter(Step::learned_topic).count();
        long completedAdditionalTheory = additionalSteps.stream().filter(Step::learned_theory).count();

        return String.format("""
                        ================================
                        Track %d
                        ================================
                        Knowledge-map:          %d
                        Topics                  %d/%d
                        Projects:               %d/%d
                        Theory:                 %d/%d
                        Steps:                  %d/%d
                        ================================
                        Additional topics:      %d/%d
                        Additional theory:      %d/%d
                        Additional steps:       %d/%d
                        ================================
                        All completed topics:   %d
                        All completed projects: %d
                        All completed theory:   %d
                        All solved steps:       %d
                        ================================""",
                TRACK,
                topic.getTopics().size(),
                progressObj.get("learned_topics_count").getAsInt(),
                topic.getDescendants().size(),
                progressObj.getAsJsonArray("completed_projects").size(),
                projects.size(),
                completedTheory,
                topic.getDescendants().size(),
                stepsSolved,
                stepsSolved + stepsUnresolved,
                completedAdditionalTopics,
                additionalSteps.size(),
                completedAdditionalTheory,
                additionalSteps.size(),
                stepsAdditionalSolved,
                stepsAdditionalSolved + stepsAdditionalUnresolved,
                currentObj.get("passed_topics").getAsInt(),
                currentObj.get("passed_projects").getAsInt(),
                currentObj.get("passed_theories").getAsInt(),
                currentObj.get("passed_problems").getAsInt());
    }

    // Print the latest statistics
    public void printStats() {

    }

    private JsonObject getProgress(WebDriver driver, String urlTopic) {
        driver.get(urlTopic);

        // Get page content as text
        String jsonProgress = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement progressElement = JsonParser.parseString(jsonProgress);
        return progressElement.getAsJsonObject()
                .getAsJsonArray("progresses").get(0).getAsJsonObject();
    }

    public static JsonObject getCurrent(WebDriver driver) {
        driver.get("https://hyperskill.org/api/profiles/current?format=json");

        // Get page content as text
        String jsonCurrent = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement currentElement = JsonParser.parseString(jsonCurrent);
        return currentElement.getAsJsonObject()
                .getAsJsonArray("profiles").get(0).getAsJsonObject();
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
    public static <T> void saveToFile(List<T> list, String path) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path);

        // Write updated data to file
        try {
            FileWriter writer = new FileWriter(file);
            gson.toJson(list, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}