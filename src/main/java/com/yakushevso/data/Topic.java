package com.yakushevso.data;

import java.util.List;

public record Topic(List<String> topics, List<String> descendants) {

    public List<String> getDescendants() {
        return descendants;
    }

    public List<String> getTopics() {
        return topics;
    }
}
