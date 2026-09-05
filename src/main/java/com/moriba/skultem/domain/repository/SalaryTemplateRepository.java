package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.SalaryTemplate;

public interface SalaryTemplateRepository {
    void save(SalaryTemplate domain);

    Optional<SalaryTemplate> findByIdAndSchoolId(String id, String schoolId);

    Page<SalaryTemplate> search(String schoolId, String query, Pageable pageable);

    Page<SalaryTemplate> findAllBySchoolId(String schoolId, Pageable pageable);

    void deleteByIdAndSchoolId(String id, String schoolId);
}
