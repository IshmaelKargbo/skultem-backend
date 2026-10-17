package com.moriba.skultem.domain.repository;

import java.util.List;

// The bulk operations behind editing an assessment template that classes are already using.
public interface TemplateMaintenanceRepository {

    // How far the template has been used: classes assessed with it, tests that went for approval (or beyond), and scores
    // somebody has marked. Tests that are merely locked, waiting for their term, don't count.
    record Usage(List<String> classNames, long submittedTests, long markedScores) {
        public boolean inUse() {
            return !classNames.isEmpty();
        }
    }

    Usage usage(String schoolId, String templateId);

    // Scores somebody has marked in one assessment (any class, any term).
    long markedScoresOf(String assessmentId);

    // Every existing score of an assessment takes the new weight (the weighted mark is worked out from it).
    int updateScoreWeight(String assessmentId, int weight);

    // Removes an assessment with its tests, scores, CA entries and approval requests.
    void deleteAssessmentCascade(String assessmentId);
}
