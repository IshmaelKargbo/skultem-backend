package com.moriba.skultem.application.dto;

import java.util.List;
import java.util.Map;

// Result of ComputeClassAttentionUseCase. students lists everyone who needs a look - including
// students on "watch" (a recent dip the term as a whole doesn't back up) - but flaggedCount counts
// only those at NEEDS_ATTENTION or CRITICAL, which is what the class badge and dashboard report.
public record ClassAttentionDTO(
        int totalStudents,
        int flaggedCount,
        int watchCount,
        List<StudentAttentionDTO> students,
        // The cut-offs behind the flags: a term average below passMark, or recent attendance below
        // attendanceThreshold (%), so the page can say what "needs attention" means for this class.
        int passMark,
        double attendanceThreshold,
        // studentId -> average of their approved scores this term, for every student who has one - the number
        // the academic flag is judged on.
        Map<String, Double> averages,
        // The term those averages cover; null when no term is active.
        String termName) {
}
