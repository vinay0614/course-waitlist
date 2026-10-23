package com.vinay.aiassistance.courseregistration.model;

import java.util.Set;

public record Learner(String learnerId, Set<String> completedPrerequisites, boolean activeStudent) {
    public Learner {
        completedPrerequisites = Set.copyOf(completedPrerequisites);
    }

    public boolean hasCompleted(String prerequisite) {
        return completedPrerequisites.contains(prerequisite);
    }
}
