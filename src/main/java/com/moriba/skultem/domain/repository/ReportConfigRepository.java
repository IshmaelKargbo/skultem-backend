package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ReportConfig;

public interface ReportConfigRepository {
    void save(ReportConfig domain);

    Optional<ReportConfig> findByIdAndSchoolId(String id, String schoolId);

    Page<ReportConfig> findAllBySchoolId(String schoolId, Pageable pageable);

    void delete(ReportConfig domain);
}
