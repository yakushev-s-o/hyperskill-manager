package com.yakushevso.data;

public record Statistics(int knowledgeMap, int topicsLearned, int topicAll,
                         int projectsLearned, int projectsAll, long theoryLearned,
                         int theoryAll, int stepsLearned, int stepsAll,
                         long additionalTopicsLearned, int additionalTopicsAll,
                         long additionalTheoryLearned, int additionalTheoryAll,
                         int additionalStepsLearned, int additionalStepsAll,
                         int allCompletedTopics, int allCompletedProjects,
                         int allCompletedTheory, int allSolvedSteps) {
}
