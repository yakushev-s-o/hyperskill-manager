package com.yakushevso.data;

import java.util.List;

public record Project(int id, String url, String title,
                      List<String> stages_ids) {

    public int getId() {
        return id;
    }

    public List<String> getStages_ids() {
        return stages_ids;
    }
}
