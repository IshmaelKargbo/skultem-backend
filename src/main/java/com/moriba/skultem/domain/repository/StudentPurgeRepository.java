package com.moriba.skultem.domain.repository;

// Hard-deletes a student and everything hanging off them - the opposite of the normal soft
// lifecycle (withdraw/expel), for a student entered by mistake.
public interface StudentPurgeRepository {

    // Money already collected for this student - a recorded payment, or a material sale with anything paid.
    boolean hasRecordedPayments(String schoolId, String studentId);

    Purged purge(String schoolId, String studentId);

    record Purged(int enrollments, int fees, int assessments) {
    }
}
