package com.yakushevso.data;

import java.util.List;

public record Data(Topic topic_relations, List<Project> projects,
                   List<Step> steps, List<Step> stepsAdditional) {

    public Topic getTopic_relations() {
        return topic_relations;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public List<Step> getSteps() {
        return steps;
    }
}
