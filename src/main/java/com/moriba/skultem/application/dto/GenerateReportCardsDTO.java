package com.moriba.skultem.application.dto;

import java.util.List;

// assessmentIds: only these of the term's assessments count (null/empty = all of them).
// sectionId / streamId: limit to one section / stream of the class (JSS 1 A, not all of JSS 1); blank = all.
// wholeYear: every term of an academic year, all assessments - the year is academicYearId, or the one
// termId belongs to.
public record GenerateReportCardsDTO(String classId, String termId, boolean includeAttendance,
        boolean includeRanking, List<String> assessmentIds, boolean wholeYear, String sectionId,
        String streamId, String academicYearId) {
}
