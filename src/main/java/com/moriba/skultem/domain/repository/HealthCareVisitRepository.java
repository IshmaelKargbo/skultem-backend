package com.moriba.skultem.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;

public interface HealthCareVisitRepository {

    /** The visit list's status filter. FOLLOW_UP = still open after the student's school day ended. */
    enum Filter {
        ALL, OPEN, FOLLOW_UP, CLOSED, VOIDED
    }

    /**
     * Null bounds and blanks mean "no limit". {@code query} matches the student's name or class;
     * {@code complaint} a complaint code.
     */
    record Search(Instant from, Instant to, Filter filter, Outcome outcome, String complaint, boolean overLimitOnly,
            String studentId, String query, Instant now) {
    }

    /**
     * Saves straight away, so a second open visit for the same student (two visits started at the same moment) is
     * refused here by the database - as a DataIntegrityViolationException - rather than at commit.
     */
    void save(HealthCareVisit domain);

    Optional<HealthCareVisit> findByIdAndSchool(String id, String schoolId);

    Optional<HealthCareVisit> findByIdAndSchoolForUpdate(String id, String schoolId);

    /** The student's open visit, if any - there is at most one (V-1). */
    Optional<HealthCareVisit> findOpenByStudent(String schoolId, String studentId);

    /** The student's visits that arrived on or after {@code since}, any status, oldest first. */
    List<HealthCareVisit> findByStudentSince(String schoolId, String studentId, Instant since);

    /** Every visit of the student, newest first. */
    List<HealthCareVisit> findByStudent(String schoolId, String studentId);

    /** Visits that arrived between {@code from} (inclusive) and {@code to} (exclusive), newest first. */
    List<HealthCareVisit> findBetween(String schoolId, Instant from, Instant to);

    List<HealthCareVisit> findOpen(String schoolId);

    Page<HealthCareVisit> search(String schoolId, Search search, Pageable pageable);

    /** Whether any visit used this complaint - such a complaint can be hidden but not deleted (C-3). */
    boolean existsWithComplaint(String schoolId, String code);
}
