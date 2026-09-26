package com.moriba.skultem.domain.repository;

// Hard-deletes a class (or one of its sessions) that was set up by mistake, together with the setup
// hanging off it - class masters, teacher/subject assignments, timetable, schemes of work. Only ever
// used for one nobody has been enrolled in.
public interface ClassPurgeRepository {

    // Students placed in the class (any year), report cards issued for it - anything that would be lost
    // with real records. Zero for a class that can be deleted.
    int recordsInClass(String schoolId, String classId);

    // Fees charged to the class that students have already been billed for or paid against.
    boolean hasBilledFees(String schoolId, String classId);

    void purgeClass(String schoolId, String classId);

    // Students placed in this one session (class + section + stream + year).
    int studentsInSession(String schoolId, String sessionId);

    void purgeSession(String schoolId, String sessionId);
}
