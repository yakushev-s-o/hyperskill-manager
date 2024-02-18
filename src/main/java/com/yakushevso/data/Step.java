package com.yakushevso.data;

import java.util.List;

public record Step(int id, int topic, boolean learnedTopic, boolean skippedTopic,
                   float capacityTopic, String url, String theory, boolean learnedTheory,
                   List<String> stepListTrue, List<String> stepListFalse) {
}
