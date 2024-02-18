package com.yakushevso.data;

import java.util.List;

public record Data(Topic topicRelations, List<Project> projects,
                   List<Step> steps, List<Step> stepsAdditional) {
}
