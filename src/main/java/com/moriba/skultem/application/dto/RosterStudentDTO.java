package com.moriba.skultem.application.dto;

/**
 * @param average          The student's overall % for the year (mean of each subject's per-term
 *                         weighted total), or null if nothing has been scored yet.
 * @param suggestedOutcome "PROMOTE"/"REPEAT" from comparing average to the school's configured
 *                         pass mark, or null when there's no average or no pass mark configured -
 *                         a starting point for the class master, who can still override it.
 */
public record RosterStudentDTO(String studentId, String enrollmentId, String studentName, String admissionNumber,
        Double average, String suggestedOutcome) {
}
