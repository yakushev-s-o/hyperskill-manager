package com.yakushevso.data;

import java.util.List;

public class Data {
    Topic topic_relations;
    List<Project> projects;
    List<Step> steps;

    public Data(Topic topic_relations, List<Project> projects, List<Step> steps) {
        this.topic_relations = topic_relations;
        this.projects = projects;
        this.steps = steps;
    }

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
