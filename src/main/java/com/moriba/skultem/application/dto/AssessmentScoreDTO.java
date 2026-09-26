package com.moriba.skultem.application.dto;

public record AssessmentScoreDTO(String id, String name, String assessment, String term, String student, String teacher, String subject, String clazz,
                Integer score, Integer weight, Integer weightScore, Integer level, String status, String grade, String trend, Boolean passed,
                // Only for an assessment opened as continuous assessment (CA + formal test); null otherwise.
                Continuous continuous) {

        public AssessmentScoreDTO(String id, String name, String assessment, String term, String student, String teacher, String subject, String clazz,
                        Integer score, Integer weight, Integer weightScore, Integer level, String status, String grade, String trend, Boolean passed) {
                this(id, name, assessment, term, student, teacher, subject, clazz, score, weight, weightScore, level, status, grade,
                                trend, passed, null);
        }

        // The structure this assessment was opened with (frozen - see ClassSubjectAssessmentLifeCycle) and this
        // student's CA recordings: caEntryScores[i] is recording i+1, null while not recorded yet.
        public record Continuous(String structure, Integer caPercentage, Integer formalPercentage, String caFrequency,
                        String caUnit, Integer caEntries, Integer configVersion, Integer caScore, Integer formalScore,
                        // The points each component contributes: CA 24 (of 30) + test 56 (of 70) = 80.
                        Double caPoints, Double formalPoints,
                        java.util.List<Integer> caEntryScores,
                        // The CA recordings were submitted as complete - the formal test can now be entered.
                        boolean caSubmitted,
                        // Recording slots that are locked (completed for every student): no longer editable.
                        java.util.List<Integer> lockedWeeks) {
        }
}
