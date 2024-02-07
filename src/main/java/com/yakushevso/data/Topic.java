package com.yakushevso.data;

import java.util.List;

public record Topic(List<Integer> topics, List<Integer> descendants) {

    public List<Integer> getTopics() {
        return topics;
    }

    public List<Integer> getDescendants() {
        return descendants;
    }
}
