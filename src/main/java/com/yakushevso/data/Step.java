package com.yakushevso.data;

import java.util.List;

public record Step(int id, int topic, String url, String theory, List<String> stepListTrue,
                   List<String> stepListFalse) {

    public List<String> getStepListTrue() {
        return stepListTrue;
    }

    public List<String> getStepListFalse() {
        return stepListFalse;
    }

    public int getId() {
        return id;
    }

    public int getTopic() {return topic;}
}
