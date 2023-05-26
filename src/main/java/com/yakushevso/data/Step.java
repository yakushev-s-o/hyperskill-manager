package com.yakushevso.data;

import java.util.List;

public class Step {
    int id;
    String theory;
    List<String> stepListTrue;
    List<String> stepListFalse;

    public Step(int id, String theory, List<String> stepListTrue, List<String> stepListFalse) {
        this.id = id;
        this.theory = theory;
        this.stepListTrue = stepListTrue;
        this.stepListFalse = stepListFalse;
    }

    public List<String> getStepListTrue() {
        return stepListTrue;
    }

    public int getId() {
        return id;
    }
}
