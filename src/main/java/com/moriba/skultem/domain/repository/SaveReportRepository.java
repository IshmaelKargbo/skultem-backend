package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.SaveReport;


public interface SaveReportRepository {
    void save(SaveReport domain);

    boolean existsByName(String name, String schoolId);

    Optional<SaveReport> findByIdAndSchoolId(String id, String schoolId);

    Page<SaveReport> findAllBySchoolId(String schoolId, Pageable pageable);

    void delete(SaveReport domain);
}
