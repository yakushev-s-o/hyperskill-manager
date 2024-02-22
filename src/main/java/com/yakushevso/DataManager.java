package com.yakushevso;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.yakushevso.data.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataManager {
    private final int TRACK;
    private final int USER;
    private static final Logger log = LoggerFactory.getLogger(DataManager.class);

    public DataManager(UserSession userSession) {
        TRACK = userSession.getTrack();
        USER = userSession.getAccount().id();
    }

    // Get track data and write to file
    public void getData(WebDriver driver) {
        log.info("Starting data collection process for track: {}", TRACK);

        try {
            Topic topic = getTopics(driver);
            List<Project> projects = getProjects(driver);
            List<Step> steps = getSteps(driver, topic);
            List<Step> additionalSteps = getSteps(driver, getAdditionalTopics(driver, topic, steps));

            Data data = new Data(topic, projects, steps, additionalSteps);
            String dataListPath = "src/main/resources/data-list-" + TRACK + ".json";
            saveToFile(data, dataListPath);
            log.info("Data saved successfully to {}", dataListPath);

            List<Statistics> statistics = getStatistics(driver, topic, projects, steps, additionalSteps);
            String statisticsPath = "src/main/resources/statistics-" + USER + "-" + TRACK + ".json";
            saveToFile(statistics, statisticsPath);
            log.info("Statistics saved successfully to {}", statisticsPath);
        } catch (Exception e) {
            log.error("An error occurred during data collection: {}", e.getMessage(), e);
        }
    }

    // Get the list of topics
    private Topic getTopics(WebDriver driver) {
        log.info("Starting to retrieve topics for track: {}", TRACK);
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

        log.info("[1/4] Topic data has been received successfully. Total topics: {}", listTopic.size());
        return new Topic(listTopic, listDescendants);
    }

    // Get a list of projects
    private List<Project> getProjects(WebDriver driver) {
        log.info("Starting to retrieve projects for track: {}", TRACK);
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

                projectList.add(new Project(id, completed, "https://hyperskill.org/projects/" + id,
                        title, stagesIds));
            }
        }

        log.info("[2/4] Project data has been received successfully. Total topics: {}", projectList.size());
        return projectList;
    }

    // Get a list of topics and tasks
    private List<Step> getSteps(WebDriver driver, Topic topics) {
        log.info("Starting to retrieve steps for track: {}", TRACK);
        List<Step> listSteps = new ArrayList<>();

        for (Integer topic : topics.descendants()) {
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
                    "https://hyperskill.org/learn/step/" + theory, titleTheory,
                    learnedTheory, listStepTrue, listStepFalse));
        }

        log.info("[3/4] Steps data has been received successfully. Total topics: {}", listSteps.size());
        return listSteps;
    }

    // Get a list of topics and tasks outside track
    private Topic getAdditionalTopics(WebDriver driver, Topic topics, List<Step> steps) {
        log.info("Starting to retrieve additional topics for track: {}", TRACK);
        Set<Integer> followerList = new HashSet<>();
        List<Integer> additionalTopic = new ArrayList<>();

        for (Integer topic : topics.descendants()) {
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
        Set<Integer> topicSet = new HashSet<>(topics.topics());

        // Formatting the Steps in HashSet
        Set<Integer> stepSet = new HashSet<>();
        for (Step step : steps) {
            stepSet.add(step.topic());
        }

        // Removing duplicates
        for (Integer follower : followerList) {
            if (!topicSet.contains(follower) && !stepSet.contains(follower)) {
                additionalTopic.add(follower);
            }
        }

        log.info("[4/4] Additional step data has been received successfully. Total topics: {}", additionalTopic.size());
        return new Topic(null, additionalTopic);
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

    // Get statistics of the received data
    private List<Statistics> getStatistics(WebDriver driver, Topic topic,
                                           List<Project> projects, List<Step> steps,
                                           List<Step> additionalSteps) {
        List<Statistics> statisticsList = getFileData(new TypeToken<List<Statistics>>() {
        }.getType(), "src/main/resources/statistics-" + USER + "-" + TRACK + ".json");

        JsonObject currentObj = getCurrent(driver).getAsJsonObject("gamification");
        JsonObject progressObj = getProgress(driver, "https://hyperskill.org/api/progresses"
                + "/track-" + TRACK + "?format=json");

        int stepsSolved = steps.stream().mapToInt(countStep -> countStep
                .stepListTrue().size()).sum();
        int stepsUnresolved = steps.stream().mapToInt(countStep -> countStep
                .stepListFalse().size()).sum();
        int stepsAdditionalSolved = additionalSteps.stream().mapToInt(countStep -> countStep
                .stepListTrue().size()).sum();
        int stepsAdditionalUnresolved = additionalSteps.stream().mapToInt(countStep -> countStep
                .stepListFalse().size()).sum();

        int knowledgeMap = topic.topics().size();
        int topicsLearned = progressObj.get("learned_topics_count").getAsInt();
        int topicAll = topic.descendants().size();
        int projectsLearned = progressObj.getAsJsonArray("completed_projects").size();
        int projectsAll = projects.size();
        long theoryLearned = steps.stream().filter(Step::learnedTheory).count();
        int theoryAll = topic.descendants().size();
        int stepsAll = stepsSolved + stepsUnresolved;
        long additionalTopicsLearned = additionalSteps.stream().filter(Step::learnedTopic).count();
        int additionalTopicsAll = additionalSteps.size();
        long additionalTheoryLearned = additionalSteps.stream().filter(Step::learnedTheory).count();
        int additionalTheoryAll = additionalSteps.size();
        int additionalStepsAll = stepsAdditionalSolved + stepsAdditionalUnresolved;
        int allCompletedTopics = currentObj.get("passed_topics").getAsInt();
        int allCompletedProjects = currentObj.get("passed_projects").getAsInt();
        int allCompletedTheory = currentObj.get("passed_theories").getAsInt();
        int allSolvedSteps = currentObj.get("passed_problems").getAsInt();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        String dataNow = formatter.format(LocalDateTime.now());

        if (statisticsList == null) {
            statisticsList = new ArrayList<>();
        }

        statisticsList.add(new Statistics(dataNow, knowledgeMap, topicsLearned, topicAll,
                projectsLearned, projectsAll, theoryLearned, theoryAll, stepsSolved,
                stepsAll, additionalTopicsLearned, additionalTopicsAll,
                additionalTheoryLearned, additionalTheoryAll, stepsAdditionalSolved,
                additionalStepsAll, allCompletedTopics, allCompletedProjects,
                allCompletedTheory, allSolvedSteps));

        return statisticsList;
    }

    // Print the latest statistics
    public void printStats(int lastStats) {
        log.info("Attempting to print the last {} statistics for user: {} and track: {}", lastStats, USER, TRACK);

        try {
            List<Statistics> statisticsList = getFileData(new TypeToken<List<Statistics>>() {
                    }.getType(),
                    "src/main/resources/statistics-" + USER + "-" + TRACK + ".json");

            if (statisticsList == null || statisticsList.isEmpty()) {
                log.warn("Statistics list is empty or null. No data to display.");
                System.out.println("There are no statistics, update the data!");
                return;
            }

            log.debug("Statistics data loaded successfully. Preparing to display the last {} entries.", lastStats);

            // Output data from the end of the list
            int start = Math.max(statisticsList.size() - lastStats, 0);
            for (int i = statisticsList.size() - 1; i >= start; i--) {
                System.out.printf("""
                                ================================
                                Track %d (%s)
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
                                ================================
                                """,
                        TRACK,
                        statisticsList.get(i).data(),
                        statisticsList.get(i).knowledgeMap(),
                        statisticsList.get(i).topicsLearned(),
                        statisticsList.get(i).topicAll(),
                        statisticsList.get(i).projectsLearned(),
                        statisticsList.get(i).projectsAll(),
                        statisticsList.get(i).theoryLearned(),
                        statisticsList.get(i).theoryAll(),
                        statisticsList.get(i).stepsLearned(),
                        statisticsList.get(i).stepsAll(),
                        statisticsList.get(i).additionalTopicsLearned(),
                        statisticsList.get(i).additionalTopicsAll(),
                        statisticsList.get(i).additionalTheoryLearned(),
                        statisticsList.get(i).additionalTheoryAll(),
                        statisticsList.get(i).additionalStepsLearned(),
                        statisticsList.get(i).additionalStepsAll(),
                        statisticsList.get(i).allCompletedTopics(),
                        statisticsList.get(i).allCompletedProjects(),
                        statisticsList.get(i).allCompletedTheory(),
                        statisticsList.get(i).allSolvedSteps());
            }

            log.info("Statistics for the last {} entries displayed successfully.", lastStats);
        } catch (Exception e) {
            log.error("Failed to load or display statistics: {}", e.getMessage(), e);
        }
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
        log.debug("Loading data of the {} type from a file: {}", type, path);
        Gson gson = new Gson();
        File file = new File(path);
        T result = null;

        if (file.exists() && file.length() != 0) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                JsonElement jsonElement = gson.fromJson(reader, JsonElement.class);
                if (jsonElement != null) {
                    if (jsonElement.isJsonArray()) {
                        // Read the list of objects
                        result = gson.fromJson(jsonElement, type);
                        log.debug("Successfully loaded a list of objects from {}", path);
                    } else {
                        // Read single object
                        result = gson.fromJson(jsonElement.getAsJsonObject(), type);
                        log.debug("Successfully loaded a single object from {}", path);
                    }
                } else {
                    log.warn("No JSON content found in file {}", path);
                }
            } catch (IOException e) {
                log.error("Failed to load data from file {}: {}", path, e.getMessage(), e);
            }
        } else {
            log.warn("File does not exist or is empty: {}", path);
        }

        return result;
    }

    // Save the object to a JSON file
    public static void saveToFile(Object object, String path) {
        log.debug("Attempt to save data to a file: {}", path);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path);

        // Write updated data to file
        try {
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(object, writer);
                log.info("The data was successfully saved to a file: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to save data to file {}: {}", path, e.getMessage(), e);
        }
    }
}