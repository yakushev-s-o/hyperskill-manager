package com.yakushevso;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.yakushevso.data.Answer;
import com.yakushevso.data.Data;
import com.yakushevso.data.Matrix;
import com.yakushevso.data.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.yakushevso.Util.*;

public class Automation {
    // Get all the correct answers and save them to a file one by one
    public void getAnswers() {
        int userId = getCurrent().get("id").getAsInt();
        Gson gson = new Gson();
        Data data;

        try (FileReader reader = new FileReader(DATA_PATH)) {
            data = gson.fromJson(reader, Data.class);

            for (Step steps : data.getSteps()) {
//                for (String step : steps.getStepListTrue()) {
                String[] test = new String[]{"18464", "3480", "4709", "3211",
                        "2512", "3224", "8301", "2248", "3478", "34802", "40118"};
                for (String step : test) {
                    // Skip if there is a match between links in the file
                    if (getFileData(new TypeToken<List<Answer>>() {
                    }.getType(), JSON_PATH) == null) {
                        driver.get(SITE_LINK + "learn/step/" + step);
                        waitDownloadElement("//div[@class='step-problem']");
                        delay(500);
                        List<Answer> listAnswers = new ArrayList<>();
                        saveToFile(getAnswer(userId, step), listAnswers, JSON_PATH);
                    } else if (!isMatchLink(step)) {
                        driver.get(SITE_LINK + "learn/step/" + step);
                        waitDownloadElement("//div[@class='step-problem']");
                        delay(500);
                        List<Answer> listAnswers = getFileData(new TypeToken<List<Answer>>() {
                        }.getType(), JSON_PATH);
                        saveToFile(getAnswer(userId, step), listAnswers, JSON_PATH);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get the correct answer using the appropriate method
    private Answer getAnswer(int userId, String step) {
        String page = SITE_LINK + "learn/step/" + step;
        String text = driver.findElement(By.xpath("//div[@class='tw-text-lg']")).getText();

        if (text.equals("Select one option from the list")) {
            return new Answer(page, 1, getTestSingle(userId, step));
        } else if (text.equals("Select one or more options from the list")) {
            return new Answer(page, 2, getTestMultiple(userId, step));
        } else if (text.contains("Write a program in")) {
            return new Answer(page, 3, getCode(userId, step));
        } else if (text.equals("Enter a number")) {
            return new Answer(page, 4, getTextNum(userId, step));
        } else if (text.equals("Enter a short text")) {
            return new Answer(page, 5, getTextShort(userId, step));
        } else if (text.equals("Match the items from left and right columns")) {
            return new Answer(page, 6, getMatch(userId, step));
        } else if (text.equals("Put the items in the correct order")) {
            return new Answer(page, 7, getSort(userId, step));
        } else if (text.equals("Choose one or more options for each row")
                || text.equals("Choose one option for each row")) {
            return new Answer(page, 8, getMatrix(userId, step));
        }

        return new Answer(page, 0, "ERROR: ANSWER_NOT_FOUND");
    }

    // Fill in the correct answers from the file on the site
    public void sendAnswers() {
        List<Answer> answers = getFileData(new TypeToken<List<Answer>>() {
        }.getType(), JSON_PATH);

        for (Answer answer : answers) {
            if (!answer.isChecked()) {
                driver.get(answer.getUrl());

                try {
                    waitDownloadElement("//div[@class='step-problem']");
                } catch (Exception e) {
                    System.out.println("Error step = " + answer.getUrl());
                    continue;
                }

                delay(500);

                if (checkButtons()) {
                    switch (answer.getMode()) {
                        case 1 -> sendTestSingle(answer.getAnswerStr());
                        case 2 -> sendTestMultiple(answer.getAnswerArr());
                        case 3 -> sendCode(answer.getAnswerStr());
                        case 4 -> sendTextNum(answer.getAnswerStr());
                        case 5 -> sendTextShort(answer.getAnswerStr());
                        case 6 -> sendMatch(answer.getAnswerListArr());
                        case 7 -> sendSort(answer.getAnswerArr());
                        case 8 -> sendMatrix(answer.getMatrixAnswer());
                    }

                    clickOnButtonSend();
                }

                // Set value checked
                if (waitDownloadElement("//strong[@class='text-success' and text()=' Correct. ']")) {
                    setChecked(answer);
                }

                delay(500);
            }
        }
    }

    // Check the buttons, if there is "continue", then return "false", the rest perform actions and return "true"
    private boolean checkButtons() {
        List<WebElement> elements = driver.findElements(By.xpath("//button[@type='button'][@click-event-part='description']"));

        for (WebElement element : elements) {
            String attribute = element.getAttribute("click-event-target");
            Actions actions = new Actions(driver);

            switch (attribute) {
                case "retry" -> {
                    actions.moveToElement(element).click().perform();

                    waitDownloadElement("//button[@id='sendBtn']");
                }
                case "reset" -> {
                    actions.moveToElement(element).click().perform();

                    waitDownloadElement("//button[@class='btn btn-dark']");

                    WebElement confirm = driver.findElement(By.xpath("//button[@class='btn btn-dark']"));
                    actions.moveToElement(confirm).click().perform();

                    waitDownloadElement("//button[@id='sendBtn']");
                }
                case "continue" -> {
                    return false;
                }
            }
        }

        return true;
    }

    // Set the "check" value in the object to "true"
    private void setChecked(Answer a) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        List<Answer> answers = getFileData(new TypeToken<List<Answer>>() {
        }.getType(), JSON_PATH);

        for (Answer answer : answers) {
            if (a.getUrl().equals(answer.getUrl())) {
                answer.setChecked(true);
            }
        }

        try {
            FileWriter writer = new FileWriter(JSON_PATH);
            gson.toJson(answers, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Check the link for a match in the file
    private boolean isMatchLink(String page) {
        List<Answer> answers = getFileData(new TypeToken<List<Answer>>() {
        }.getType(), JSON_PATH);

        for (Answer answer : answers) {
            if (answer.getUrl().equals(SITE_LINK + "learn/step/" + page)) {
                return true;
            }
        }

        return false;
    }

    // Get one response from the test
    private String getTestSingle(int userId, String step) {
        List<String> optionsList = new ArrayList<>();
        List<Boolean> choicesList = new ArrayList<>();

        String urlAttempts = "https://hyperskill.org/api/attempts?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlAttempts);

        // Get page content as text
        String jsonAttempts = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement attemptsElement = JsonParser.parseString(jsonAttempts);
        JsonObject attemptsObject = attemptsElement.getAsJsonObject();
        JsonArray attempts = attemptsObject.getAsJsonArray("attempts");

        // Forming a list of possible answers
        for (JsonElement element : attempts) {
            // Selecting an active attempt
            if (element.getAsJsonObject().get("status").getAsString().equals("active")) {
                JsonObject dataset = element.getAsJsonObject().get("dataset").getAsJsonObject();
                JsonArray options = dataset.getAsJsonArray("options");

                for (JsonElement option : options) {
                    optionsList.add(option.getAsString());
                }
            }
        }

        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String jsonSubmissions = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(jsonSubmissions);
        JsonArray submissions = submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");

        // Determining the correct answer
        for (JsonElement element : submissions) {
            // Choosing the right answer
            if (element.getAsJsonObject().get("status").getAsString().equals("correct")) {
                JsonArray choices = element.getAsJsonObject().get("reply")
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
        }

        return "";
    }

    // Select one answer in the test
    private void sendTestSingle(String answer) {
        waitDownloadElement("//label[@class='custom-control-label']");

        Actions actions = new Actions(driver);
        List<WebElement> elements = driver.findElements(By.xpath("//label[@class='custom-control-label']"));

        for (WebElement text : elements) {
            if (text.getText().equals(answer)) {
                actions.moveToElement(text).click().perform();
            }
        }
    }

    // Get multiple responses from the test
    private String[] getTestMultiple(int userId, String step) {
        List<String> optionsList = new ArrayList<>();
        List<Boolean> choicesList = new ArrayList<>();
        List<String> answersList = new ArrayList<>();

        String urlAttempts = "https://hyperskill.org/api/attempts?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlAttempts);

        // Get page content as text
        String jsonAttempts = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement attemptsElement = JsonParser.parseString(jsonAttempts);
        JsonObject attemptsObject = attemptsElement.getAsJsonObject();
        JsonArray attempts = attemptsObject.getAsJsonArray("attempts");

        // Forming a list of possible answers
        for (JsonElement element : attempts) {
            // Selecting an active attempt
            if (element.getAsJsonObject().get("status").getAsString().equals("active")) {
                JsonObject dataset = element.getAsJsonObject().get("dataset").getAsJsonObject();
                JsonArray options = dataset.getAsJsonArray("options");

                // Forming a list of possible answers
                for (JsonElement option : options) {
                    optionsList.add(option.getAsString());
                }
            }
        }

        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String jsonSubmissions = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(jsonSubmissions);
        JsonArray submissions = submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");

        // Determining the correct answers
        for (JsonElement element : submissions) {
            // Choosing the right answer
            if (element.getAsJsonObject().get("status").getAsString().equals("correct")) {
                JsonArray choices = element.getAsJsonObject().get("reply")
                        .getAsJsonObject().getAsJsonArray("choices");

                for (JsonElement choice : choices) {
                    choicesList.add(choice.getAsBoolean());
                }

                for (int i = 0; i < choicesList.size(); i++) {
                    if (choicesList.get(i)) {
                        answersList.add(optionsList.get(i));
                    }
                }
            }
        }

        return answersList.toArray(new String[0]);
    }

    // Select multiple answers in the test
    private void sendTestMultiple(String[] answer) {
        waitDownloadElement("//label[@class='custom-control-label']");

        for (String text : answer) {
            Actions actions = new Actions(driver);
            WebElement input;

            if (text.contains("\n")) {
                StringBuilder str = new StringBuilder("//label[@class='custom-control-label']");
                String[] textAnswer = text.split("\n");

                for (String value : textAnswer) {
                    str.append("[contains(normalize-space(),'").append(value).append("')]");
                }

                input = driver.findElement(By.xpath(String.valueOf(str)));
            } else {
                if (text.contains("'")) {
                    input = driver.findElement(By.xpath("//label[@class='custom-control-label']" +
                            "[normalize-space()=\"" + text + "\"]"));
                } else {
                    input = driver.findElement(By.xpath("//label[@class='custom-control-label']" +
                            "[normalize-space()='" + text + "']"));
                }
            }

            actions.moveToElement(input).click().perform();
        }
    }

    // Get the response from the field with the code
    private String getCode(int userId, String step) {
        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String json = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(json);
        JsonArray submissions = submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");

        // Determining the correct answer
        for (JsonElement element : submissions) {
            // Choosing the right answer
            if (element.getAsJsonObject().get("status").getAsString().equals("correct")) {
                return element.getAsJsonObject().get("reply")
                        .getAsJsonObject().get("code").getAsString();
            }
        }

        return "";
    }

    // Write the answer in the field with the code
    private void sendCode(String code) {
        waitDownloadElement("//div[@class='cm-content']");

        WebElement element = driver.findElement(By.xpath("//div[@class='cm-content']"));
        element.clear();

        JavascriptExecutor executor = (JavascriptExecutor) driver;
        // escape() - escape characters in code
        String escapedText = (String) executor.executeScript("return escape(arguments[0]);", code);
        // decodeURIComponent() - decode the escaped code
        executor.executeScript("arguments[0].innerText = decodeURIComponent('" + escapedText + "');", element);

    }

    // Get response from text field
    private String getTextNum(int userId, String step) {
        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String json = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(json);
        JsonArray submissions = submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");

        // Determining the correct answer
        for (JsonElement element : submissions) {
            // Choosing the right answer
            if (element.getAsJsonObject().get("status").getAsString().equals("correct")) {
                return element.getAsJsonObject().get("reply")
                        .getAsJsonObject().get("number").getAsString();
            }
        }

        return "";
    }

    // Write the answer to the text field
    private void sendTextNum(String answer) {
        waitDownloadElement("//input[@type='number']");

        WebElement element = driver.findElement(By.xpath("//input[@type='number']"));
        element.sendKeys(answer);
    }

    // Get response from text field
    private String getTextShort(int userId, String step) {
        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String json = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(json);
        JsonArray submissions = submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");

        // Determining the correct answer
        for (JsonElement element : submissions) {
            // Choosing the right answer
            if (element.getAsJsonObject().get("status").getAsString().equals("correct")) {
                return element.getAsJsonObject().get("reply")
                        .getAsJsonObject().get("text").getAsString();
            }
        }

        return "";
    }

    // Write the answer to the text field
    private void sendTextShort(String answer) {
        waitDownloadElement("//textarea");

        WebElement element = driver.findElement(By.xpath("//textarea"));
        element.sendKeys(answer);
    }

    // Get the list of correct answers from the matching test
    private String[][] getMatch(int userId, String step) {
        List<JsonElement> optionsList = new ArrayList<>();
        List<Integer> choicesList = new ArrayList<>();
        List<String[]> answersList = new ArrayList<>();

        String urlAttempts = "https://hyperskill.org/api/attempts?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlAttempts);

        // Get page content as text
        String jsonAttempts = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement attemptsElement = JsonParser.parseString(jsonAttempts);
        JsonArray attempts = attemptsElement.getAsJsonObject()
                .getAsJsonArray("attempts");

        // Forming a list of possible answers
        for (JsonElement element : attempts) {
            // Selecting an active attempt
            if (element.getAsJsonObject().get("status").getAsString().equals("active")) {
                JsonObject dataset = element.getAsJsonObject().get("dataset").getAsJsonObject();
                JsonArray pairs = dataset.getAsJsonArray("pairs");

                // Forming a list of possible answers
                for (JsonElement option : pairs) {
                    optionsList.add(option);
                }
            }
        }

        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String jsonSubmissions = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(jsonSubmissions);
        JsonArray submissions = submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");

        // Determining the correct answers
        for (JsonElement element : submissions) {
            // Choosing the right answer
            if (element.getAsJsonObject().get("status").getAsString().equals("correct")) {
                JsonArray ordering = element.getAsJsonObject().get("reply")
                        .getAsJsonObject().getAsJsonArray("ordering");

                for (JsonElement order : ordering) {
                    choicesList.add(order.getAsInt());
                }

                for (int i = 0; i < optionsList.size(); i++) {
                    answersList.add(new String[]{
                            optionsList.get(i).getAsJsonObject().get("first").getAsString(),
                            optionsList.get(choicesList.get(i)).getAsJsonObject().get("second").getAsString()});
                }
            }
        }

        return answersList.toArray(new String[0][]);
    }

    // Select responses in matched test
    private void sendMatch(String[][] correctAnswers) {
        for (int i = 1; i <= correctAnswers.length; i++) {
            String question = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div/div/div[1]" +
                    "/div[" + i + "]/span";
            WebElement element1 = driver.findElement(By.xpath(question));
            String text1 = element1.getText();

            String[] res = null;

            for (String[] ans : correctAnswers) {
                res = ans;

                if (res[0].equals(text1)) {
                    break;
                }
            }

            boolean checkTrue = true;

            while (checkTrue) {
                for (int j = 1; j <= correctAnswers.length; j++) {
                    String answer = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div/div/div[2]" +
                            "/div/div[" + j + "]/div/span";
                    String upArrow = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div/div/div[2]" +
                            "/div/div[" + j + "]/div/div[2]/button[" + 1 + "]";
                    String downArrow = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div/div/div[2]" +
                            "/div/div[" + j + "]/div/div[2]/button[" + 2 + "]";
                    WebElement element2 = driver.findElement(By.xpath(answer));
                    String text2 = element2.getText();

                    if (text2.equals(res[1])) {
                        if (i != j) {
                            Actions actions = new Actions(driver);
                            WebElement arrow = driver.findElement(By.xpath(i < j ? upArrow : downArrow));
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
    private String[] getSort(int userId, String step) {
        List<String> optionsList = new ArrayList<>();
        List<Integer> choicesList = new ArrayList<>();
        List<String> answersList = new ArrayList<>();

        String urlAttempts = "https://hyperskill.org/api/attempts?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlAttempts);

        // Get page content as text
        String jsonAttempts = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement attemptsElement = JsonParser.parseString(jsonAttempts);
        JsonArray attempts = attemptsElement.getAsJsonObject().getAsJsonArray("attempts");

        // Forming a list of possible answers
        for (JsonElement element : attempts) {
            // Selecting an active attempt
            if (element.getAsJsonObject().get("status").getAsString().equals("active")) {
                JsonObject dataset = element.getAsJsonObject().get("dataset").getAsJsonObject();
                JsonArray options = dataset.getAsJsonArray("options");

                // Forming a list of possible answers
                for (JsonElement option : options) {
                    optionsList.add(option.getAsString());
                }
            }
        }

        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String jsonSubmissions = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(jsonSubmissions);
        JsonArray submissions = submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");

        // Determining the correct answers
        for (JsonElement element : submissions) {
            // Choosing the right answer
            if (element.getAsJsonObject().get("status").getAsString().equals("correct")) {
                JsonArray ordering = element.getAsJsonObject().get("reply")
                        .getAsJsonObject().getAsJsonArray("ordering");

                for (JsonElement order : ordering) {
                    choicesList.add(order.getAsInt());
                }

                // Pre-filling an array with a null value
                answersList = new ArrayList<>(Collections.nCopies(optionsList.size(), null));

                for (int i = 0; i < optionsList.size(); i++) {
                    answersList.set(choicesList.get(i), optionsList.get(i));
                }
            }
        }

        return answersList.toArray(new String[0]);
    }

    // Select answers in the sorted test
    private void sendSort(String[] correctAnswers) {
        for (int i = 1; i <= correctAnswers.length; i++) {
            boolean checkTrue = true;

            while (checkTrue) {
                for (int j = 1; j <= correctAnswers.length; j++) {
                    String upArrow = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div[1]/div/div/span" +
                            "/div[" + j + "]/div[3]/button[" + 1 + "]";
                    String downArrow = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div[1]/div/div/span" +
                            "/div[" + j + "]/div[3]/button[" + 2 + "]";
                    String answer = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div[1]/div/div/span" +
                            "/div[" + j + "]/div[2]/span";
                    WebElement element = driver.findElement(By.xpath(answer));

                    if (element.getText().equals(correctAnswers[i - 1])) {
                        if (i != j) {
                            Actions actions = new Actions(driver);
                            WebElement arrow = driver.findElement(By.xpath(i < j ? upArrow : downArrow));
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
    private List<Matrix> getMatrix(int userId, String step) {
        List<Matrix> answersList = new ArrayList<>();

        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String jsonSubmissions = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(jsonSubmissions);
        JsonArray submissions = submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");

        // Determining the correct answers
        for (JsonElement element : submissions) {
            // Choosing the right answer
            if (element.getAsJsonObject().get("status").getAsString().equals("correct")) {
                JsonArray choices = element.getAsJsonObject().get("reply")
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
            }
        }

        return answersList;
    }

    // Select the correct answers in the matrix test
    private void sendMatrix(List<Matrix> matrixList) {
        WebElement thead = driver.findElement(By.tagName("thead"));
        List<WebElement> head = thead.findElements(By.tagName("tr"));
        List<WebElement> columnsArr = head.get(0).findElements(By.tagName("th"));

        WebElement tbody = driver.findElement(By.tagName("tbody"));
        List<WebElement> rowArr = tbody.findElements(By.tagName("tr"));

        for (int i = 1; i < rowArr.size() + 1; i++) {
            for (int j = 1; j < columnsArr.size(); j++) {
                List<WebElement> nameRow = rowArr.get(i - 1).findElements(By.tagName("td"));

                for (Matrix matrix : matrixList) {
                    if (matrix.getName_row().equals(nameRow.get(0).getText()) &&
                            matrix.getName_columns().equals(columnsArr.get(j).getText()) && matrix.isCheck()) {
                        String s = "/html/body/div/main/div/div/div[1]/div/div[4]/div/div/div[1]/div[1]/div/table" +
                                "/tbody/tr[" + i + "]/td[" + (j + 1) + "]/div/div";
                        WebElement checkbox = driver.findElement(By.xpath(s));
                        checkbox.click();
                    }
                }
            }
        }
    }

    // Click on the "Send" button
    private void clickOnButtonSend() {
        Actions actions = new Actions(driver);
        WebElement signInButton = driver.findElement(By.xpath("//button[@id='sendBtn']"));
        actions.moveToElement(signInButton).click().perform();
    }
}
