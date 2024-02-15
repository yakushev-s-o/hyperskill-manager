package com.yakushevso.data;

public class Statistics {
    private String knowledge_map;
    private String topics;
    private String projects;
    private String theory;
    private String steps;
    private String additional_topics;
    private String additional_theory;
    private String additional_steps;
    private String all_completed_topics;
    private String all_completed_projects;
    private String all_completed_theory;
    private String all_solved_steps;

    public Statistics(String knowledge_map, String topics, String projects,
                      String theory, String steps, String additional_topics,
                      String additional_theory, String additional_steps,
                      String all_completed_topics, String all_completed_projects,
                      String all_completed_theory, String all_solved_steps) {
        this.knowledge_map = knowledge_map;
        this.topics = topics;
        this.projects = projects;
        this.theory = theory;
        this.steps = steps;
        this.additional_topics = additional_topics;
        this.additional_theory = additional_theory;
        this.additional_steps = additional_steps;
        this.all_completed_topics = all_completed_topics;
        this.all_completed_projects = all_completed_projects;
        this.all_completed_theory = all_completed_theory;
        this.all_solved_steps = all_solved_steps;
    }

    public String getKnowledge_map() {
        return knowledge_map;
    }

    public void setKnowledge_map(String knowledge_map) {
        this.knowledge_map = knowledge_map;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public String getProjects() {
        return projects;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }

    public String getTheory() {
        return theory;
    }

    public void setTheory(String theory) {
        this.theory = theory;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getAdditional_topics() {
        return additional_topics;
    }

    public void setAdditional_topics(String additional_topics) {
        this.additional_topics = additional_topics;
    }

    public String getAdditional_theory() {
        return additional_theory;
    }

    public void setAdditional_theory(String additional_theory) {
        this.additional_theory = additional_theory;
    }

    public String getAdditional_steps() {
        return additional_steps;
    }

    public void setAdditional_steps(String additional_steps) {
        this.additional_steps = additional_steps;
    }

    public String getAll_completed_topics() {
        return all_completed_topics;
    }

    public void setAll_completed_topics(String all_completed_topics) {
        this.all_completed_topics = all_completed_topics;
    }

    public String getAll_completed_projects() {
        return all_completed_projects;
    }

    public void setAll_completed_projects(String all_completed_projects) {
        this.all_completed_projects = all_completed_projects;
    }

    public String getAll_completed_theory() {
        return all_completed_theory;
    }

    public void setAll_completed_theory(String all_completed_theory) {
        this.all_completed_theory = all_completed_theory;
    }

    public String getAll_solved_steps() {
        return all_solved_steps;
    }

    public void setAll_solved_steps(String all_solved_steps) {
        this.all_solved_steps = all_solved_steps;
    }
}
