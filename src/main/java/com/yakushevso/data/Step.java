package com.yakushevso.data;

import java.util.List;

public final class Step {
    private final int id;
    private final int topic;
    private final boolean learnedTopic;
    private boolean skippedTopic;
    private final float capacityTopic;
    private final String url;
    private final String theory;
    private final boolean learnedTheory;
    private final List<String> stepListTrue;
    private final List<String> stepListFalse;

    public Step(int id, int topic, boolean learnedTopic, boolean skippedTopic,
                float capacityTopic, String url, String theory, boolean learnedTheory,
                List<String> stepListTrue, List<String> stepListFalse) {
        this.id = id;
        this.topic = topic;
        this.learnedTopic = learnedTopic;
        this.skippedTopic = skippedTopic;
        this.capacityTopic = capacityTopic;
        this.url = url;
        this.theory = theory;
        this.learnedTheory = learnedTheory;
        this.stepListTrue = stepListTrue;
        this.stepListFalse = stepListFalse;
    }

    public int id() {
        return id;
    }

    public int topic() {
        return topic;
    }

    public boolean learnedTopic() {
        return learnedTopic;
    }

    public boolean skippedTopic() {
        return skippedTopic;
    }

    public float capacityTopic() {
        return capacityTopic;
    }

    public String url() {
        return url;
    }

    public String theory() {
        return theory;
    }

    public boolean learnedTheory() {
        return learnedTheory;
    }

    public List<String> stepListTrue() {
        return stepListTrue;
    }

    public List<String> stepListFalse() {
        return stepListFalse;
    }

    public void setSkippedTopic(boolean skippedTopic) {
        this.skippedTopic = skippedTopic;
    }
}
