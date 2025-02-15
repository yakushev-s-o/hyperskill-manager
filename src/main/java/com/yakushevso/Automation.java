package com.yakushevso;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.yakushevso.data.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Automation {
    private final WebDriver DRIVER;
    private final int TRACK;
    private final int USER_ID;
    private static final Logger log = LoggerFactory.getLogger(Automation.class);

    public Automation(WebDriver driver, UserSession userSession) {
        DRIVER = driver;
        TRACK = userSession.getTrack();
        USER_ID = userSession.getAccount().id();
    }

    public void unSkipThemes(WebDriver driver) {
        log.info("Starting to unSkip themes for track: {}", TRACK);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Data data = DataManager.getFileData(Data.class, "src/main/resources/data-list-" + TRACK + ".json");

        if (data == null) {
            log.error("Failed to load data for track: {}. It is not possible to continue canceling the skipping of themes.", TRACK);
            return;
        }

        for (Step step : data.steps()) {
            if (step.skippedTopic()) {
                try {
                    log.debug("Loading theme page: {}", step.id());
                    driver.get("https://hyperskill.org/learn/step/" + step.id());
                    String unSkipButton = "//button//span[text() = 'Un skip this topic']";
                    Util.waitDownloadElement(driver, unSkipButton);
                    Util.delay(1000);
                    WebElement element = driver.findElement(By.xpath(unSkipButton));
                    new Actions(driver).moveToElement(element).click().perform();
                    log.debug("Skipping a topic has been canceled: {}", step.id());
                    Util.delay(1000);
                    String skipButton = "//button//span[text() = 'Skip this topic']\n";
                    Util.waitDownloadElement(driver, skipButton);

                    step.setSkippedTopic(false);

                    try {
                        FileWriter writer = new FileWriter("src/main/resources/data-list-" + TRACK + ".json");
                        gson.toJson(data, writer);
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (Exception e) {
                    log.error("Failed to unSkip theme {}: {}", step.id(), e.getMessage(), e);
                }
            }
        }

        log.info("The skipping of topics has been successfully canceled for track: {}", TRACK);
    }

    // Get all the correct answers and save them to a file one by one
    public void getAnswers() {
        log.info("Starting the process to get answers for track: {}", TRACK);

        Gson gson = new Gson();
        File file = new File("src/main/resources/answer-list-" + TRACK + ".json");
        List<Answer> listAnswers = new ArrayList<>();
        boolean fileNotExistsOrEmpty = !file.exists() || file.length() == 0;

        try (FileReader reader = new FileReader("src/main/resources/data-list-" + TRACK + ".json")) {
            Data data = gson.fromJson(reader, Data.class);

            for (Step steps : data.steps()) {
                for (String step : steps.stepListTrue()) {
                    if (fileNotExistsOrEmpty || isNotMatchStep(step)) {
                        if (!fileNotExistsOrEmpty) {
                            listAnswers = DataManager.getFileData(new TypeToken<List<Answer>>() {
                            }.getType(), "src/main/resources/answer-list-" + TRACK + ".json");
                        }

                        Answer answer = getAnswer(step);

                        if (answer == null) {
                            log.error("Answer not found for step: https://hyperskill.org/learn/step/{}", step);
                            System.out.println("ANSWER_NOT_FOUND: https://hyperskill.org/learn/step/" + step);
                            continue;
                        }

                        listAnswers.add(answer);
                        DataManager.saveToFile(listAnswers, "src/main/resources/answer-list-" + TRACK + ".json");
                        log.info("Answer for step: {} added and saved.", step);
                        fileNotExistsOrEmpty = false;
                    }
                }
            }

            log.info("All answers have been successfully retrieved and saved for track: {}", TRACK);
            System.out.println("The answers have been successfully received!");
        } catch (IOException e) {
            log.error("Failed to load data list file for track: {}. Error: {}", TRACK, e.getMessage(), e);
            System.out.println("File \"answer-list-" + TRACK + ".json\" was not found!");
        }
    }

    // Defining the type of question
    private String getType(String step) {
        String url = "https://hyperskill.org/api/steps/" + step + "?format=json";

        DRIVER.get(url);

        // Get page content as text
        String pageSource = DRIVER.findElement(By.tagName("pre")).getText();

        // Get JSON object with data
        JsonElement jsonElement = JsonParser.parseString(pageSource);
        JsonElement block = jsonElement.getAsJsonObject().get("steps")
                .getAsJsonArray().get(0).getAsJsonObject().get("block");
        String name = block.getAsJsonObject().get("name").getAsString();

        if ("choice".equals(name)) {
            if (block.getAsJsonObject().get("options")
                    .getAsJsonObject().get("is_multiple_choice").getAsBoolean()) {
                return "multiple_choice";
            }

            return "choice";
        }

        return name;
    }

    // Get the correct answer using the appropriate method
    private Answer getAnswer(String step) {
        log.debug("Retrieving answer for step: {}", step);

        String page = "https://hyperskill.org/learn/step/" + step;
        String type = getType(step);

        try {
            switch (type) {
                case "choice" -> {
                    String answerChoice = getTestSingle(step);
                    if (answerChoice != null) {
                        log.info("'Single' choice answer retrieved for step ID: {}", step);
                        return new Answer(page, 1, answerChoice);
                    }
                }
                case "multiple_choice" -> {
                    String[] answerMultiple = getTestMultiple(step);
                    if (answerMultiple != null) {
                        log.info("'Multiple' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 2, answerMultiple);
                    }
                }
                case "code" -> {
                    String answerCode = getCode(step);
                    if (answerCode != null) {
                        log.info("'Code' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 3, answerCode);
                    }
                }
                case "number" -> {
                    String answerNumber = getTextNum(step);
                    if (answerNumber != null) {
                        log.info("'Number' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 4, answerNumber);
                    }
                }
                case "string" -> {
                    String answerString = getTextShort(step);
                    if (answerString != null) {
                        log.info("'String' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 5, answerString);
                    }
                }
                case "matching" -> {
                    String[][] answerMatching = getMatch(step);
                    if (answerMatching != null) {
                        log.info("'Matching' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 6, answerMatching);
                    }
                }
                case "sorting" -> {
                    String[] answerSorting = getSort(step);
                    if (answerSorting != null) {
                        log.info("'Sorting' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 7, answerSorting);
                    }
                }
                case "table" -> {
                    List<Matrix> answerTable = getMatrix(step);
                    if (answerTable != null) {
                        log.info("'Table' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 8, answerTable);
                    }
                }
                case "parsons" -> {
                    String[][] answerParsons = getLines(step);
                    if (answerParsons != null) {
                        log.info("'Parsons' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 9, answerParsons);
                    }
                }
                case "fill-blanks" -> {
                    String[] answerFill = getComponents(step);
                    if (answerFill != null) {
                        log.info("'Fill-blanks' choice answers retrieved for step ID: {}", step);
                        return new Answer(page, 10, answerFill);
                    }
                }
                default -> log.warn("Unhandled question type '{}' for step ID: {}", type, step);
            }
        } catch (Exception e) {
            log.error("Error while retrieving answer for step ID: {}. Type: {}. Error: {}", step, type, e.getMessage(), e);
        }

        log.warn("No answer found or failed to retrieve answer for step ID: {}. Type: {}", step, type);
        return null;
    }

    // Fill in the correct answers from the file on the site
    public void sendAnswers() {
        log.info("Starting the process to send answers for track: {}", TRACK);

        List<Answer> answers = DataManager.getFileData(new TypeToken<List<Answer>>() {
        }.getType(), "src/main/resources/answer-list-" + TRACK + ".json");

        if (answers != null) {
            for (Answer answer : answers) {
                if (!answer.isChecked()) {
                    DRIVER.get(answer.getUrl());

                    try {
                        Util.waitDownloadElement(DRIVER, "//div[@class='step-problem']");
                    } catch (Exception e) {
                        log.error("Page loading error: {}", answer.getUrl());
                        System.out.println("LOADING_ERROR: " + answer.getUrl());
                        continue;
                    }

                    Util.delay(500);

                    if (checkButtons()) {
                        try {
                            switch (answer.getMode()) {
                                case 1 -> sendTestSingle(answer.getAnswerStr());
                                case 2 -> sendTestMultiple(answer.getAnswerArr());
                                case 3 -> sendCode(answer.getAnswerStr());
                                case 4 -> sendTextNum(answer.getAnswerStr());
                                case 5 -> sendTextShort(answer.getAnswerStr());
                                case 6 -> sendMatch(answer.getAnswerListArr());
                                case 7 -> sendSort(answer.getAnswerArr());
                                case 8 -> sendMatrix(answer.getMatrixAnswer());
                                case 9 -> sendLines(answer.getAnswerListArr());
                                case 10 -> sendComponents(answer.getAnswerArr());
                            }
                        } catch (Exception e) {
                            log.error("Error sending the response: {} : {}. Error: {}", answer.getUrl(), e.getMessage(), e);
                            System.out.println("RESPONSE_ERROR: " + answer.getUrl());
                            continue;
                        }

                        clickOnButtonSend();
                    }

                    // Set value checked
                    if (Util.waitDownloadElement(DRIVER, "//strong[@class='text-success' and text()=' Correct. ']")) {
                        setChecked(answer);
                    }

                    Util.delay(500);
                }
            }

            log.info("All answers have been successfully sent for track: {}", TRACK);
            System.out.println("The answers have been successfully received!");
        } else {
            log.warn("The list of answers is empty. Update the data.");
            System.out.println("File \"data-list-" + TRACK + ".json\" does not exist. Please update the data.");
        }
    }

    // Check the buttons, if there is "continue", then return "false", the rest perform actions and return "true"
    private boolean checkButtons() {
        List<WebElement> elements = DRIVER.findElements(By.xpath("//button[@type='button'][@click-event-part='description']"));

        for (WebElement element : elements) {
            String attribute = element.getAttribute("click-event-target");
            Actions actions = new Actions(DRIVER);

            switch (Objects.requireNonNull(attribute)) {
                case "retry" -> {
                    actions.moveToElement(element).click().perform();

                    Util.waitDownloadElement(DRIVER, "//button[@id='sendBtn']");
                }
                case "reset" -> {
                    actions.moveToElement(element).click().perform();

                    Util.waitDownloadElement(DRIVER, "//button[@class='btn btn-dark']");

                    WebElement confirm = DRIVER.findElement(By.xpath("//button[@class='btn btn-dark']"));
                    actions.moveToElement(confirm).click().perform();

                    Util.waitDownloadElement(DRIVER, "//button[@id='sendBtn']");
                }
                case "continue" -> {
                    return false;
                }
            }
        }

        return true;
    }

    // Click on the "Send" button
    private void clickOnButtonSend() {
        Actions actions = new Actions(DRIVER);
        WebElement signInButton = DRIVER.findElement(By.xpath("//button[@id='sendBtn']"));
        actions.moveToElement(signInButton).click().perform();
        Util.delay(500);
    }

    // Set the "check" value in the object to "true"
    private void setChecked(Answer a) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        List<Answer> answers = DataManager.getFileData(new TypeToken<List<Answer>>() {
        }.getType(), "src/main/resources/answer-list-" + TRACK + ".json");

        for (Answer answer : answers) {
            if (a.getUrl().equals(answer.getUrl())) {
                answer.setChecked(true);
            }
        }

        try {
            FileWriter writer = new FileWriter("src/main/resources/answer-list-" + TRACK + ".json");
            gson.toJson(answers, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Check the link for a match in the file
    private boolean isNotMatchStep(String page) {
        List<Answer> answers = DataManager.getFileData(new TypeToken<List<Answer>>() {
        }.getType(), "src/main/resources/answer-list-" + TRACK + ".json");

        for (Answer answer : answers) {
            if (answer.getUrl().equals("https://hyperskill.org/learn/step/" + page)) {
                return false;
            }
        }

        return true;
    }

    // Remove unnecessary characters
    private String formatText(String text) {
        text = text.replaceAll("&amp;", "&")
                .replaceAll("&gt;", ">")
                .replaceAll("&lt;", "<")
                .replaceAll("&le;", "≤")
                .replaceAll("&ge;", "≥")
                .replaceAll("&#x27;", "'")
                .replaceAll("<br>", "")
                .replaceAll("<b>", "")
                .replaceAll("</b>", "")
                .replaceAll("<code>", "")
                .replaceAll("</code>", "")
                .replaceAll("&quot;", "\"");

        if (text.startsWith(" ")) {
            text = text.substring(1);
        }

        if (text.endsWith(" ")) {
            text = text.substring(0, text.length() - 1);
        }

        return text;
    }

    // Get a list of attempts
    private JsonArray getAttempts(String step) {
        String urlAttempts = "https://hyperskill.org/api/attempts?format=json&page_size=100&step="
                + step + "&user=" + USER_ID;

        DRIVER.get(urlAttempts);

        // Get page content as text
        String jsonAttempts = DRIVER.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement attemptsElement = JsonParser.parseString(jsonAttempts);
        return attemptsElement.getAsJsonObject()
                .getAsJsonArray("attempts");
    }

    // Get a list of submissions
    private JsonArray getSubmissions(String step) {
        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + USER_ID;

        DRIVER.get(urlSubmissions);

        // Get page content as text
        String jsonSubmissions = DRIVER.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(jsonSubmissions);
        return submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");
    }

    // Get one response from the test
    private String getTestSingle(String step) {
        List<String> optionsList = new ArrayList<>();
        List<Boolean> choicesList = new ArrayList<>();

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray options = dataset.getAsJsonArray("options");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {
            for (JsonElement option : options) {
                optionsList.add(formatText(option.getAsString()));
            }

            // Determining the correct answer
            JsonArray submissions = getSubmissions(step);
            JsonArray choices = submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().getAsJsonArray("choices");

            for (JsonElement choice : choices) {
                choicesList.add(choice.getAsBoolean());
            }

            for (int i = 0; i < choicesList.size(); i++) {
                if (choicesList.get(i)) {
                    return optionsList.get(i);
                }
            }
        }

        return null;
    }

    // Select one answer in the test
    private void sendTestSingle(String answer) {
        Util.waitDownloadElement(DRIVER, "//label[@class='custom-control-label']");

        Actions actions = new Actions(DRIVER);
        WebElement element;

        if (answer.contains("'")) {
            element = DRIVER.findElement(By.xpath("//label[@class='custom-control-label']" +
                    "[.//*[normalize-space()=\"" + answer + "\"]]"));
        } else {
            element = DRIVER.findElement(By.xpath("//label[@class='custom-control-label']" +
                    "[.//*[normalize-space()='" + answer + "']]"));
        }

        actions.moveToElement(element).click().perform();
    }

    // Get multiple responses from the test
    private String[] getTestMultiple(String step) {
        List<String> optionsList = new ArrayList<>();
        List<Boolean> choicesList = new ArrayList<>();
        List<String> answersList = new ArrayList<>();

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray options = dataset.getAsJsonArray("options");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            for (JsonElement option : options) {
                optionsList.add(formatText(option.getAsString()));
            }

            // Determining the correct answers
            JsonArray submissions = getSubmissions(step);
            JsonArray choices = submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().getAsJsonArray("choices");

            for (JsonElement choice : choices) {
                choicesList.add(choice.getAsBoolean());
            }

            for (int i = 0; i < choicesList.size(); i++) {
                if (choicesList.get(i)) {
                    answersList.add(optionsList.get(i));
                }
            }

            return answersList.toArray(new String[0]);
        }

        return null;
    }

    // Select multiple answers in the test
    private void sendTestMultiple(String[] answers) {
        Util.waitDownloadElement(DRIVER, "//label[@class='custom-control-label']");

        for (String answer : answers) {
            Actions actions = new Actions(DRIVER);
            WebElement element;

            if (answer.contains("'")) {
                element = DRIVER.findElement(By.xpath("//label[@class='custom-control-label']" +
                        "[.//*[normalize-space()=\"" + answer + "\"]]"));
            } else {
                element = DRIVER.findElement(By.xpath("//label[@class='custom-control-label']" +
                        "[.//*[normalize-space()='" + answer + "']]"));
            }

            actions.moveToElement(element).click().perform();
        }
    }

    // Get the response from the field with the code
    private String getCode(String step) {
        JsonArray attempts = getAttempts(step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answer
            JsonArray submissions = getSubmissions(step);

            return submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().get("code").getAsString();
        }

        return null;
    }

    // Write the answer in the field with the code
    private void sendCode(String code) {
        Util.waitDownloadElement(DRIVER, "//div[@class='cm-content'][@contenteditable='true']");

        WebElement element = DRIVER.findElement(By.xpath("//div[@class='cm-content'][@contenteditable='true']"));
        element.clear();

        JavascriptExecutor executor = (JavascriptExecutor) DRIVER;
        // escape() - escape characters in code
        String escapedText = (String) executor.executeScript("return escape(arguments[0]);", code);
        // decodeURIComponent() - decode the escaped code
        executor.executeScript("arguments[0].innerText = decodeURIComponent('" + escapedText + "');", element);

    }

    // Get response from text field
    private String getTextNum(String step) {
        JsonArray attempts = getAttempts(step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answer
            JsonArray submissions = getSubmissions(step);

            return submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().get("number").getAsString();
        }

        return null;
    }

    // Write the answer to the text field
    private void sendTextNum(String answer) {
        Util.waitDownloadElement(DRIVER, "//input[@type='number']");

        WebElement element = DRIVER.findElement(By.xpath("//input[@type='number']"));
        element.sendKeys(answer);
    }

    // Get response from text field
    private String getTextShort(String step) {
        JsonArray attempts = getAttempts(step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answer
            JsonArray submissions = getSubmissions(step);

            return submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().get("text").getAsString();
        }

        return null;
    }

    // Write the answer to the text field
    private void sendTextShort(String answer) {
        Util.waitDownloadElement(DRIVER, "//textarea");

        WebElement element = DRIVER.findElement(By.xpath("//textarea"));
        element.sendKeys(answer);
    }

    // Get the list of correct answers from the matching test
    private String[][] getMatch(String step) {
        List<JsonElement> optionsList = new ArrayList<>();
        List<Integer> choicesList = new ArrayList<>();
        List<String[]> answersList = new ArrayList<>();

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray pairs = dataset.getAsJsonArray("pairs");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Forming a list of possible answers
            for (JsonElement option : pairs) {
                optionsList.add(option);
            }

            // Determining the correct answers
            JsonArray submissions = getSubmissions(step);
            JsonArray ordering = submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().getAsJsonArray("ordering");

            for (JsonElement order : ordering) {
                choicesList.add(order.getAsInt());
            }

            for (int i = 0; i < optionsList.size(); i++) {
                answersList.add(new String[]{
                        formatText(optionsList.get(i).getAsJsonObject().get("first").getAsString()),
                        optionsList.get(choicesList.get(i)).getAsJsonObject().get("second").getAsString()});
            }

            return answersList.toArray(new String[0][]);
        }

        return null;
    }

    // Select responses in matched test
    private void sendMatch(String[][] correctAnswers) {
        for (int i = 1; i <= correctAnswers.length; i++) {
            WebElement question = DRIVER.findElement(By.xpath("//div[@class='step-problem']" +
                    "/div/div[1]/div[" + i + "]/span"));
            String textQuestion = question.getText();
            String[] res = null;

            for (String[] ans : correctAnswers) {
                res = ans;

                if (res[0].equals(textQuestion)) {
                    break;
                }
            }

            boolean checkTrue = true;
            while (checkTrue) {
                for (int j = 1; j <= correctAnswers.length; j++) {
                    String upArrow = "//div[@class='step-problem']" +
                            "/div/div[2]/div/div[" + j + "]/div/div[2]/button[1]";
                    String downArrow = "//div[@class='step-problem']" +
                            "/div/div[2]/div/div[" + j + "]/div/div[2]/button[2]";
                    WebElement answer = DRIVER.findElement(By.xpath("//div[@class='step-problem']" +
                            "//div/div[2]/div/div[" + j + "]/div/span"));
                    String textAnswer = answer.getText();

                    if (textAnswer.equals(res[1])) {
                        if (i != j) {
                            Actions actions = new Actions(DRIVER);
                            WebElement arrow = DRIVER.findElement(By.xpath(i < j ? upArrow : downArrow));
                            actions.moveToElement(arrow).click().perform();
                        } else {
                            checkTrue = false;
                        }
                    }
                }
            }
        }
    }

    // Get a list of correct answers from the test with sorting
    private String[] getSort(String step) {
        List<String> optionsList = new ArrayList<>();
        List<Integer> choicesList = new ArrayList<>();

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray options = dataset.getAsJsonArray("options");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Forming a list of possible answers
            for (JsonElement option : options) {
                optionsList.add(option.getAsString());
            }

            // Determining the correct answers
            JsonArray submissions = getSubmissions(step);
            JsonArray ordering = submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().getAsJsonArray("ordering");

            for (JsonElement order : ordering) {
                choicesList.add(order.getAsInt());
            }

            // Pre-filling an array with a null value
            List<String> answersList = new ArrayList<>(Collections.nCopies(optionsList.size(), null));

            for (int i = 0; i < optionsList.size(); i++) {
                answersList.set(choicesList.get(i), optionsList.get(i));
            }

            return answersList.toArray(new String[0]);
        }

        return null;
    }

    // Select answers in the sorted test
    private void sendSort(String[] correctAnswers) {
        for (int i = 1; i <= correctAnswers.length; i++) {

            boolean checkTrue = true;
            while (checkTrue) {
                for (int j = 1; j <= correctAnswers.length; j++) {
                    String upArrow = "//div[@class='step-problem']//div[" + j + "]/div[2]/button[1]";
                    String downArrow = "//div[@class='step-problem']//div[" + j + "]/div[2]/button[2]";
                    WebElement answer = DRIVER.findElement(By.xpath(
                            "//div[@class='step-problem']//div[" + j + "]/div[1]/div[2]/span"));

                    if (answer.getText().equals(correctAnswers[i - 1])) {
                        if (i != j) {
                            Actions actions = new Actions(DRIVER);
                            WebElement arrow = DRIVER.findElement(By.xpath(i < j ? upArrow : downArrow));
                            actions.moveToElement(arrow).click().perform();
                        } else {
                            checkTrue = false;
                        }
                    }
                }
            }
        }
    }

    // Get the matrix of correct answers from the test
    private List<Matrix> getMatrix(String step) {
        List<Matrix> answersList = new ArrayList<>();

        JsonArray attempts = getAttempts(step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answers
            JsonArray submissions = getSubmissions(step);
            JsonArray choices = submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().getAsJsonArray("choices");

            for (JsonElement choice : choices) {
                String name_row = choice.getAsJsonObject().get("name_row").getAsString();
                String name_columns;
                boolean check;

                JsonArray columns = choice.getAsJsonObject().get("columns").getAsJsonArray();
                for (JsonElement colum : columns) {
                    name_columns = colum.getAsJsonObject().get("name").getAsString();
                    check = colum.getAsJsonObject().get("answer").getAsBoolean();

                    answersList.add(new Matrix(name_row, name_columns, check));
                }
            }

            return answersList;
        }

        return null;
    }

    // Select the correct answers in the matrix test
    private void sendMatrix(List<Matrix> matrixList) {
        WebElement thead = DRIVER.findElement(By.tagName("thead"));
        List<WebElement> head = thead.findElements(By.tagName("tr"));
        List<WebElement> columnsArr = head.get(0).findElements(By.tagName("th"));

        WebElement tbody = DRIVER.findElement(By.tagName("tbody"));
        List<WebElement> rowArr = tbody.findElements(By.tagName("tr"));

        for (int i = 1; i < rowArr.size() + 1; i++) {
            for (int j = 1; j < columnsArr.size(); j++) {
                List<WebElement> nameRow = rowArr.get(i - 1).findElements(By.tagName("td"));

                for (Matrix matrix : matrixList) {
                    if (matrix.nameRow().equals(nameRow.get(0).getText()) &&
                            matrix.nameColumns().equals(columnsArr.get(j)
                                    .getText()) && matrix.check()) {
                        String s = "//div[@class='table-problem']" +
                                "/table/tbody/tr[" + i + "]/td[" + (j + 1) + "]/div/div";
                        WebElement checkbox = DRIVER.findElement(By.xpath(s));
                        checkbox.click();
                    }
                }
            }
        }
    }

    // Get a list of correct answers from the test with lines
    private String[][] getLines(String step) {
        List<String> attemptsList = new ArrayList<>();
        List<JsonElement> submissionsList = new ArrayList<>();
        List<String[]> answersList = new ArrayList<>();

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray attemptsLines = dataset.getAsJsonArray("lines");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Forming a list of possible answers
            for (JsonElement line : attemptsLines) {
                attemptsList.add(formatText(line.getAsString()));
            }

            // Determining the correct answers
            JsonArray submissions = getSubmissions(step);
            JsonArray submissionsLines = submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().getAsJsonArray("lines");

            for (JsonElement line : submissionsLines) {
                submissionsList.add(line);
            }

            for (int i = 0; i < attemptsList.size(); i++) {
                answersList.add(new String[]{
                        attemptsList.get(i),
                        submissionsList.get(i).getAsJsonObject().get("line_number").getAsString(),
                        submissionsList.get(i).getAsJsonObject().get("level").getAsString(),
                });
            }

            return answersList.toArray(new String[0][]);
        }

        return null;
    }

    // Select responses in lines test
    private void sendLines(String[][] correctAnswers) {
        for (String[] correctAnswer : correctAnswers) {
            int position = 0;

            // Determine the current line position
            for (int j = 1; j <= correctAnswers.length; j++) {
                String line = "//div[@class='parsons-problem']//div/div/div[" + j + "]/div[2]/div/pre/code";
                String textLine = DRIVER.findElement(By.xpath(line)).getText();

                if (textLine.equals(correctAnswer[0])) {
                    position = j;
                    break;
                }
            }

            boolean check = true;
            while (check) {
                String upArrow = "//div[@class='parsons-problem']//div[" + position + "]/div[3]/button[1]";
                String downArrow = "//div[@class='parsons-problem']//div[" + position + "]/div[3]/button[2]";

                // Change the line position
                if (position - 1 != Integer.parseInt(correctAnswer[1])) {
                    Actions actions = new Actions(DRIVER);
                    WebElement arrow;

                    if (position - 1 > Integer.parseInt(correctAnswer[1])) {
                        arrow = DRIVER.findElement(By.xpath(upArrow));
                        position--;
                    } else {
                        arrow = DRIVER.findElement(By.xpath(downArrow));
                        position++;
                    }

                    actions.moveToElement(arrow).click().perform();
                    Util.delay(500);
                } else {
                    String rightLevel = "//div[@class='parsons-problem']//div[" + position + "]/div[1]/button[2]";

                    // Change indentation position
                    for (int i = 0; i < Integer.parseInt(correctAnswer[2]); i++) {
                        Actions actions = new Actions(DRIVER);
                        WebElement level = DRIVER.findElement(By.xpath(rightLevel));
                        actions.moveToElement(level).click().perform();
                    }

                    check = false;
                }
            }
        }
    }

    // Get a list of correct answers from the test with components
    private String[] getComponents(String step) {
        List<String> answersList = new ArrayList<>();

        JsonArray attempts = getAttempts(step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answers
            JsonArray submissions = getSubmissions(step);
            JsonArray blanks = submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().getAsJsonArray("blanks");

            for (JsonElement blank : blanks) {
                answersList.add(blank.getAsString());
            }

            return answersList.toArray(new String[0]);
        }

        return null;
    }

    // Select responses in components test
    private void sendComponents(String[] correctAnswers) {
        for (String answer : correctAnswers) {
            Actions actions = new Actions(DRIVER);
            WebElement element = DRIVER.findElement(By.xpath(
                    "//span[@class='draggable' and text()='" + answer + "']"));
            actions.moveToElement(element).click().perform();
        }
    }
}