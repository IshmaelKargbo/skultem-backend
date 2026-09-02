package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ReportCard;

public interface ReportCardRepository {
    void save(ReportCard domain);

    Optional<ReportCard> findById(String id);

    Optional<ReportCard> findByIdAndSchoolId(String id, String schoolId);

    Optional<ReportCard> findBySchoolIdAndStudentIdAndTermId(String schoolId, String studentId, String termId);

    // Every report card a student has ever had generated, most recent first - powers the Report
    // Card tab on the student profile (all terms/years in one place, not just the current term).
    List<ReportCard> findAllBySchoolIdAndStudentId(String schoolId, String studentId);

    Page<ReportCard> search(String schoolId, String classId, String termId, String search, Pageable pageable);

    long countBySchoolId(String schoolId);

    long countBySchoolIdAndPassed(String schoolId, boolean passed);

    long sumDownloadsBySchoolId(String schoolId);
}
