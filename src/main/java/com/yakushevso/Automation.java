package com.yakushevso;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.List;

import static com.yakushevso.Util.*;

public class Automation {
    // Get all the correct answers and save them to a file one by one
    public void getAnswers() {
        Gson gson = new Gson();
        Data data;

        try (FileReader reader = new FileReader(DATA_PATH)) {
            data = gson.fromJson(reader, Data.class);

            for (Step steps : data.getSteps()) {
                for (String step : steps.getStepListTrue()) {
                    // Skip if there is a match between links in the file
                    if (getFileData(new TypeToken<List<Answer>>() {}.getType(), JSON_PATH) == null) {
                        driver.get(SITE_LINK + "learn/step/" + step);
                        waitDownloadElement("//div[@class='step-problem']");
                        delay(500);
                        List<Answer> listAnswers = new ArrayList<>();
                        saveToFile(getAnswer(step), listAnswers, JSON_PATH);
                    } else if (!isMatchLink(step)) {
                        driver.get(SITE_LINK + "learn/step/" + step);
                        waitDownloadElement("//div[@class='step-problem']");
                        delay(500);
                        List<Answer> listAnswers = getFileData(new TypeToken<List<Answer>>() {
                        }.getType(), JSON_PATH);
                        saveToFile(getAnswer(step), listAnswers, JSON_PATH);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get the correct answer using the appropriate method
    private Answer getAnswer(String step) {
        final String SINGLE = "Select one option from the list";
        final String MULTIPLE = "Select one or more options from the list";
        final String CODE = "Write a program in";
        final String TEXT_NUM = "Enter a number";
        final String TEXT_SHORT = "Enter a short text";
        final String MATCH = "Match the items from left and right columns";
        final String SORT = "Put the items in the correct order";
        final String MATRIX_MORE = "Choose one or more options for each row";
        final String MATRIX_ONE = "Choose one option for each row";

        WebElement element = driver.findElement(By.xpath("//div[@class='tw-text-lg']"));
        String page = SITE_LINK + "learn/step/" + step;
        String text = element.getText();

        if (text.equals(SINGLE)) {
            return new Answer(page, 1, getTestSingle());
        } else if (text.equals(MULTIPLE)) {
            return new Answer(page, 2, getTestMultiple());
        } else if (text.contains(CODE)) {
            return new Answer(page, 3, getCode());
        } else if (text.equals(TEXT_NUM)) {
            return new Answer(page, 4, getTextNum());
        } else if (text.equals(TEXT_SHORT)) {
            return new Answer(page, 5, getTextShort());
        } else if (text.equals(MATCH)) {
            return new Answer(page, 6, getMatch());
        } else if (text.equals(SORT)) {
            return new Answer(page, 7, getSort());
        } else if (text.equals(MATRIX_MORE) || text.equals(MATRIX_ONE)) {
            return new Answer(page, 8, getMatrix());
        }

        return new Answer(page, 0, "ANSWER_NOT_FOUND");
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
    private String getTestSingle() {
        waitDownloadElement("//div[@class='submission submission-correct']");

        List<WebElement> elements = driver.findElements(By.xpath("//input[@type='radio']"));

        for (WebElement answer : elements) {
            if (answer.getAttribute("checked") != null) {
                WebElement text = answer.findElement(By.xpath("./following-sibling::label/div"));

                return text.getText();
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
    private String[] getTestMultiple() {
        waitDownloadElement("//label[@class='custom-control-label']");

        List<String> correctAnswers = new ArrayList<>();
        List<WebElement> elements = driver.findElements(By.xpath("//input[@type='checkbox']"));

        for (WebElement answer : elements) {
            if (answer.getAttribute("checked") != null) {
                WebElement text = answer.findElement(By.xpath("./following-sibling::label/div"));
                correctAnswers.add(text.getText());
            }
        }

        return correctAnswers.toArray(new String[0]);
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
    private String getCode() {
        waitDownloadElement("//div[@class='cm-content']");

        WebElement element = driver.findElement(By.xpath("//div[@class='cm-content']"));
        return element.getText();
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
    private String getTextNum() {
        waitDownloadElement("//div[@class='submission submission-correct']");

        WebElement element = driver.findElement(By.xpath("//input[@type='number']"));
        return element.getAttribute("value");
    }

    // Write the answer to the text field
    private void sendTextNum(String answer) {
        waitDownloadElement("//input[@type='number']");

        WebElement element = driver.findElement(By.xpath("//input[@type='number']"));
        element.sendKeys(answer);
    }

    // Get response from text field
    private String getTextShort() {
        waitDownloadElement("//div[@class='submission submission-correct']");

        WebElement element = driver.findElement(By.xpath("//textarea"));
        return element.getAttribute("value");
    }

    // Write the answer to the text field
    private void sendTextShort(String answer) {
        waitDownloadElement("//textarea");

        WebElement element = driver.findElement(By.xpath("//textarea"));
        element.sendKeys(answer);
    }

    // Get the list of correct answers from the matching test
    private String[][] getMatch() {
        List<String[]> correctAnswers = new ArrayList<>();
        List<WebElement> count = driver.findElements(By.xpath("//div[@class='left-side__line']"));

        for (int i = 1; i <= count.size(); i++) {
            // /html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div[1]/div/div[1]/div[1]/span
            String question = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div[1]/div/div[1]/" +
                    "div[" + i + "]/span";
            // /html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div[1]/div/div[2]/div/div[1]/div/span
            String answer = "/html/body/div/main/div/div/div/div/div[4]/div/div/div[1]/div[1]/div/div[2]/div/" +
                    "div[" + i + "]/div/span";
            WebElement element1 = driver.findElement(By.xpath(question));
            WebElement element2 = driver.findElement(By.xpath(answer));

            String[] pairs;

            // If the text is not found, look for the image
            if (element1.getText().equals("")) {
                String questionImg = "/html/body/div[1]/div[1]/div/div/div/div[4]/div/div/div[1]/div[1]/div/div[1]" +
                        "/div[" + i + "]/span/img";
                element1 = driver.findElement(By.xpath(questionImg));
                pairs = new String[]{element1.getAttribute("src"), element2.getText()};
            } else {
                pairs = new String[]{element1.getText(), element2.getText()};
            }

            correctAnswers.add(pairs);
        }

        return correctAnswers.toArray(new String[0][]);
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
    private String[] getSort() {
        List<String> correctAnswers = new ArrayList<>();
        List<WebElement> count = driver.findElements(By.xpath("//div[@class='line-value']/span"));

        for (int i = 1; i <= count.size(); i++) {
            String answer = "/html/body/div[1]/div[1]/div/div/div/div[4]/div/div/div[1]/div[1]/div/div/span" +
                    "/div[" + i + "]/div[2]/span";
            WebElement element = driver.findElement(By.xpath(answer));
            correctAnswers.add(element.getText());
        }

        return correctAnswers.toArray(new String[0]);
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
    private List<Matrix> getMatrix() {
        WebElement thead = driver.findElement(By.tagName("thead"));
        List<WebElement> head = thead.findElements(By.tagName("tr"));
        List<WebElement> columnsArr = head.get(0).findElements(By.tagName("th"));

        WebElement tbody = driver.findElement(By.tagName("tbody"));
        List<WebElement> rowArr = tbody.findElements(By.tagName("tr"));

        List<Matrix> matrixList = new ArrayList<>();

        for (int i = 1; i < rowArr.size() + 1; i++) {
            for (int j = 1; j < columnsArr.size(); j++) {
                String s = "/html/body/div[1]/div[1]/div/div/div/div[4]/div/div/div[1]/div[1]/div/table/tbody/tr" +
                        "[" + i + "]/td[" + (j + 1) + "]/div/div";
                WebElement checkbox = driver.findElement(By.xpath(s));
                boolean check = "custom-checkbox checked disabled".equals(checkbox.getAttribute("class")) ||
                        "custom-radio checked disabled".equals(checkbox.getAttribute("class"));
                List<WebElement> nameRow = rowArr.get(i - 1).findElements(By.tagName("td"));

                matrixList.add(new Matrix(nameRow.get(0).getText(), columnsArr.get(j).getText(), check));
            }
        }

        return matrixList;
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
