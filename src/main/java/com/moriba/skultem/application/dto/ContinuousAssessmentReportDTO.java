package com.moriba.skultem.application.dto;

import java.util.List;

// The continuous-assessment (CA + formal test) report for a term: how each CA assessment is going, and which
// students the classwork already says need a look - before the formal test, when help is still cheap.
public record ContinuousAssessmentReportDTO(Summary summary, List<AssessmentRow> assessments, List<StudentRow> students,
        // Paging of `students` (the flagged list): the page shown, its size, and how many are flagged in all.
        int page, int size, int studentsTotal) {

    public record Summary(int assessments, int fullyRecorded, int inProgress, int students, int flagged,
            Integer averageCa, Integer averageFormal) {
    }

    public record AssessmentRow(String cycleId, String clazz, String subject, String assessment, String teacher,
            String status, boolean caSubmitted, int lockedWeeks, int expectedWeeks,
            // Recordings made out of all the slots every student should have.
            int recordedPercent, int students, Integer averageCa, Integer averageFormal, int improving, int declining) {
    }

    public record StudentRow(String student, String clazz, String subject, String assessment, Integer caAverage,
            Integer formalScore, String trend, List<Integer> recordings, List<String> reasons) {
    }
}
