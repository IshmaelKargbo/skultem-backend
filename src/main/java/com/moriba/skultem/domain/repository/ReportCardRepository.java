package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ReportCard;

public interface ReportCardRepository {
    void save(ReportCard domain);

    Optional<ReportCard> findById(String id);

    Optional<ReportCard> findByIdAndSchoolId(String id, String schoolId);

    Optional<ReportCard> findBySchoolIdAndStudentIdAndTermId(String schoolId, String studentId, String termId);

    Page<ReportCard> search(String schoolId, String classId, String termId, String search, Pageable pageable);

    long countBySchoolId(String schoolId);

    long countBySchoolIdAndPassed(String schoolId, boolean passed);

    long sumDownloadsBySchoolId(String schoolId);
}
