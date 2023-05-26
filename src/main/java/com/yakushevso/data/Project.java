package com.yakushevso.data;

import java.util.List;

public class Project {
    int id;
    String title;
    List<String>  stages_ids;

    public Project(int id, String title, List<String> stages_ids) {
        this.id = id;
        this.title = title;
        this.stages_ids = stages_ids;
    }

    public int getId() {
        return id;
    }

    public List<String> getStages_ids() {
        return stages_ids;
    }
}
