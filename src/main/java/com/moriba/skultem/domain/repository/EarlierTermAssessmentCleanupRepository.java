package com.moriba.skultem.domain.repository;

public interface EarlierTermAssessmentCleanupRepository {

    // Removes the assessments the students of every class using a template were given for terms before `termNumber` in one academic year - but only
    // where nobody has graded anything: a score someone has marked or recorded CA for is kept, and so are the student
    // assessment and test it belongs to. Returns how many scores were removed.
    int deleteUngradedBeforeTerm(String schoolId, String templateId, String academicYearId, int termNumber);
}
