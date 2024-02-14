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

import java.io.File;
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
        Gson gson = new Gson();
        File file = new File(JSON_PATH);
        List<Answer> listAnswers = new ArrayList<>();
        int userId = getCurrent().get("id").getAsInt();
        boolean fileNotExistsOrEmpty = !file.exists() || file.length() == 0;

        try (FileReader reader = new FileReader(DATA_PATH)) {
            Data data = gson.fromJson(reader, Data.class);

            for (Step steps : data.getSteps()) {
                for (String step : steps.getStepListTrue()) {
                    if (fileNotExistsOrEmpty || isNotMatchStep(step)) {
                        if (!fileNotExistsOrEmpty) {
                            listAnswers = getFileData(new TypeToken<List<Answer>>() {
                            }.getType(), JSON_PATH);
                        }

                        Answer answer = getAnswer(userId, step);

                        if (answer == null) {
                            System.out.println("ANSWER_NOT_FOUND: " + SITE_LINK + "learn/step/" + step);
                            continue;
                        }

                        listAnswers.add(answer);
                        saveToFile(listAnswers, JSON_PATH);
                        fileNotExistsOrEmpty = false;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Defining the type of question
    private String getType(String step) {
        String url = "https://hyperskill.org/api/steps/" + step + "?format=json";

        driver.get(url);

        // Get page content as text
        String pageSource = driver.findElement(By.tagName("pre")).getText();

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
    private Answer getAnswer(int userId, String step) {
        String page = SITE_LINK + "learn/step/" + step;
        String text = getType(step);

        if (text.equals("choice")) {
            String answer = getTestSingle(userId, step);
            if (answer != null) {
                return new Answer(page, 1, answer);
            }
        } else if (text.equals("multiple_choice")) {
            String[] answer = getTestMultiple(userId, step);
            if (answer != null) {
                return new Answer(page, 2, answer);
            }
        } else if (text.contains("code")) {
            String answer = getCode(userId, step);
            if (answer != null) {
                return new Answer(page, 3, answer);
            }
        } else if (text.equals("number")) {
            String answer = getTextNum(userId, step);
            if (answer != null) {
                return new Answer(page, 4, answer);
            }
        } else if (text.equals("string")) {
            String answer = getTextShort(userId, step);
            if (answer != null) {
                return new Answer(page, 5, answer);
            }
        } else if (text.equals("matching")) {
            String[][] answer = getMatch(userId, step);
            if (answer != null) {
                return new Answer(page, 6, answer);
            }
        } else if (text.equals("sorting")) {
            String[] answer = getSort(userId, step);
            if (answer != null) {
                return new Answer(page, 7, answer);
            }
        } else if (text.equals("table")) {
            List<Matrix> answer = getMatrix(userId, step);
            if (answer != null) {
                return new Answer(page, 8, answer);
            }
        } else if (text.equals("parsons")) {
            String[][] answer = getLines(userId, step);
            if (answer != null) {
                return new Answer(page, 9, answer);
            }
        } else if (text.equals("fill-blanks")) {
            String[] answer = getComponents(userId, step);
            if (answer != null) {
                return new Answer(page, 10, answer);
            }
        }

        return null;
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
                    System.out.println("LOADING_ERROR: " + answer.getUrl());
                    continue;
                }

                delay(500);

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
                        System.out.println("RESPONSE_ERROR: " + answer.getUrl());
                        e.printStackTrace();
                        continue;
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

    // Click on the "Send" button
    private void clickOnButtonSend() {
        Actions actions = new Actions(driver);
        WebElement signInButton = driver.findElement(By.xpath("//button[@id='sendBtn']"));
        actions.moveToElement(signInButton).click().perform();
        delay(500);
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
    private boolean isNotMatchStep(String page) {
        List<Answer> answers = getFileData(new TypeToken<List<Answer>>() {
        }.getType(), JSON_PATH);

        for (Answer answer : answers) {
            if (answer.getUrl().equals(SITE_LINK + "learn/step/" + page)) {
                return false;
            }
        }

        return true;
    }

    // Remove unnecessary characters
    private String formatText(String text) {
        text = text.replaceAll("\u0026amp;", "\u0026")
                .replaceAll("\u0026gt;", "\u003e")
                .replaceAll("\u0026lt;", "\u003c")
                .replaceAll("\u0026le;", "\u2264")
                .replaceAll("\u0026ge;", "\u2265")
                .replaceAll("\u0026#x27;", "\u0027")
                .replaceAll("\u003cbr\u003e", "")
                .replaceAll("\u003cb\u003e", "")
                .replaceAll("\u003c/b\u003e", "")
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
    private JsonArray getAttempts(int userId, String step) {
        String urlAttempts = "https://hyperskill.org/api/attempts?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlAttempts);

        // Get page content as text
        String jsonAttempts = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement attemptsElement = JsonParser.parseString(jsonAttempts);
        return attemptsElement.getAsJsonObject()
                .getAsJsonArray("attempts");
    }

    // Get a list of submissions
    private JsonArray getSubmissions(int userId, String step) {
        String urlSubmissions = "https://hyperskill.org/api/submissions?format=json&page_size=100&step="
                + step + "&user=" + userId;

        driver.get(urlSubmissions);

        // Get page content as text
        String jsonSubmissions = driver.findElement(By.tagName("pre")).getText();

        // Get JSON object from text
        JsonElement submissionsElement = JsonParser.parseString(jsonSubmissions);
        return submissionsElement.getAsJsonObject()
                .getAsJsonArray("submissions");
    }

    // Get one response from the test
    private String getTestSingle(int userId, String step) {
        List<String> optionsList = new ArrayList<>();
        List<Boolean> choicesList = new ArrayList<>();

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(userId, step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray options = dataset.getAsJsonArray("options");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {
            for (JsonElement option : options) {
                optionsList.add(formatText(option.getAsString()));
            }

            // Determining the correct answer
            JsonArray submissions = getSubmissions(userId, step);
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
        waitDownloadElement("//label[@class='custom-control-label']");

        Actions actions = new Actions(driver);
        WebElement element;

        if (answer.contains("'")) {
            element = driver.findElement(By.xpath("//label[@class='custom-control-label']" +
                    "[.//*[normalize-space()=\"" + answer + "\"]]"));
        } else {
            element = driver.findElement(By.xpath("//label[@class='custom-control-label']" +
                    "[.//*[normalize-space()='" + answer + "']]"));
        }

        actions.moveToElement(element).click().perform();
    }

    // Get multiple responses from the test
    private String[] getTestMultiple(int userId, String step) {
        List<String> optionsList = new ArrayList<>();
        List<Boolean> choicesList = new ArrayList<>();
        List<String> answersList = new ArrayList<>();

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(userId, step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray options = dataset.getAsJsonArray("options");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            for (JsonElement option : options) {
                optionsList.add(formatText(option.getAsString()));
            }

            // Determining the correct answers
            JsonArray submissions = getSubmissions(userId, step);
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
        waitDownloadElement("//label[@class='custom-control-label']");

        for (String answer : answers) {
            Actions actions = new Actions(driver);
            WebElement element;

            if (answer.contains("'")) {
                element = driver.findElement(By.xpath("//label[@class='custom-control-label']" +
                        "[.//*[normalize-space()=\"" + answer + "\"]]"));
            } else {
                element = driver.findElement(By.xpath("//label[@class='custom-control-label']" +
                        "[.//*[normalize-space()='" + answer + "']]"));
            }

            actions.moveToElement(element).click().perform();
        }
    }

    // Get the response from the field with the code
    private String getCode(int userId, String step) {
        JsonArray attempts = getAttempts(userId, step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answer
            JsonArray submissions = getSubmissions(userId, step);

            return submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().get("code").getAsString();
        }

        return null;
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
        JsonArray attempts = getAttempts(userId, step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answer
            JsonArray submissions = getSubmissions(userId, step);

            return submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().get("number").getAsString();
        }

        return null;
    }

    // Write the answer to the text field
    private void sendTextNum(String answer) {
        waitDownloadElement("//input[@type='number']");

        WebElement element = driver.findElement(By.xpath("//input[@type='number']"));
        element.sendKeys(answer);
    }

    // Get response from text field
    private String getTextShort(int userId, String step) {
        JsonArray attempts = getAttempts(userId, step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answer
            JsonArray submissions = getSubmissions(userId, step);

            return submissions.get(0).getAsJsonObject().get("reply")
                    .getAsJsonObject().get("text").getAsString();
        }

        return null;
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

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(userId, step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray pairs = dataset.getAsJsonArray("pairs");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Forming a list of possible answers
            for (JsonElement option : pairs) {
                optionsList.add(option);
            }

            // Determining the correct answers
            JsonArray submissions = getSubmissions(userId, step);
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
            WebElement question = driver.findElement(By.xpath("//div[@class='step-problem']" +
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
                    WebElement answer = driver.findElement(By.xpath("//div[@class='step-problem']" +
                            "//div/div[2]/div/div[" + j + "]/div/span"));
                    String textAnswer = answer.getText();

                    if (textAnswer.equals(res[1])) {
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

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(userId, step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray options = dataset.getAsJsonArray("options");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Forming a list of possible answers
            for (JsonElement option : options) {
                optionsList.add(option.getAsString());
            }

            // Determining the correct answers
            JsonArray submissions = getSubmissions(userId, step);
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
                    String upArrow = "//div[@class='step-problem']" +
                            "/div/span/div[" + j + "]/div[2]/button[1]";
                    String downArrow = "//div[@class='step-problem']" +
                            "/div/span/div[" + j + "]/div[2]/button[2]";
                    WebElement answer = driver.findElement(By.xpath("//div[@class='step-problem']" +
                            "/div/span/div[" + j + "]/div[1]/div[2]/span"));

                    if (answer.getText().equals(correctAnswers[i - 1])) {
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

        JsonArray attempts = getAttempts(userId, step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answers
            JsonArray submissions = getSubmissions(userId, step);
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
                            matrix.getName_columns().equals(columnsArr.get(j)
                                    .getText()) && matrix.isCheck()) {
                        String s = "//div[@class='table-problem']" +
                                "/table/tbody/tr[" + i + "]/td[" + (j + 1) + "]/div/div";
                        WebElement checkbox = driver.findElement(By.xpath(s));
                        checkbox.click();
                    }
                }
            }
        }
    }

    // Get a list of correct answers from the test with lines
    private String[][] getLines(int userId, String step) {
        List<String> attemptsList = new ArrayList<>();
        List<JsonElement> submissionsList = new ArrayList<>();
        List<String[]> answersList = new ArrayList<>();

        // Forming a list of possible answers
        JsonArray attempts = getAttempts(userId, step);
        JsonObject dataset = attempts.get(0).getAsJsonObject().get("dataset").getAsJsonObject();
        JsonArray attemptsLines = dataset.getAsJsonArray("lines");
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Forming a list of possible answers
            for (JsonElement line : attemptsLines) {
                attemptsList.add(formatText(line.getAsString()));
            }

            // Determining the correct answers
            JsonArray submissions = getSubmissions(userId, step);
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
                String line = "//div[@class='parsons-problem']//div/span/div[" + j + "]/div[2]";

                WebElement element;
                element = driver.findElement(By.xpath(line));

                if (element.getText().equals(correctAnswer[0])) {
                    position = j;
                    break;
                }
            }

            boolean check = true;
            while (check) {
                String upArrow = "//div[@class='parsons-problem']" +
                        "//div/span/div[" + position + "]/div[3]/button[1]";

                String downArrow = "//div[@class='parsons-problem']" +
                        "//div/span/div[" + position + "]/div[3]/button[2]";

                // Change the line position
                if (position - 1 != Integer.parseInt(correctAnswer[1])) {
                    Actions actions = new Actions(driver);
                    WebElement arrow;

                    if (position - 1 > Integer.parseInt(correctAnswer[1])) {
                        arrow = driver.findElement(By.xpath(upArrow));
                        position--;
                    } else {
                        arrow = driver.findElement(By.xpath(downArrow));
                        position++;
                    }

                    actions.moveToElement(arrow).click().perform();
                    delay(500);
                } else {
                    String rightLevel = "//div[@class='parsons-problem']" +
                            "//div/span/div[" + position + "]/div[1]/button[2]";

                    // Change indentation position
                    for (int i = 0; i < Integer.parseInt(correctAnswer[2]); i++) {
                        Actions actions = new Actions(driver);
                        WebElement level = driver.findElement(By.xpath(rightLevel));
                        actions.moveToElement(level).click().perform();
                    }

                    check = false;
                }
            }
        }
    }

    // Get a list of correct answers from the test with components
    private String[] getComponents(int userId, String step) {
        List<String> answersList = new ArrayList<>();

        JsonArray attempts = getAttempts(userId, step);
        String status = attempts.get(0).getAsJsonObject().get("status").getAsString();

        if ("active".equals(status)) {

            // Determining the correct answers
            JsonArray submissions = getSubmissions(userId, step);
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
            Actions actions = new Actions(driver);
            WebElement element = driver.findElement(By.xpath(
                    "//span[@class='draggable' and text()='" + answer + "']"));
            actions.moveToElement(element).click().perform();
        }
    }
}
