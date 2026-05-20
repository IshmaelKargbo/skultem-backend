package com.moriba.skultem.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.MaterialTransaction;

public interface MaterialTransactionRepository {
    void save(MaterialTransaction domain);

    Page<MaterialTransaction> findByMaterialIdAndSchool(String materialId, String schoolId, Pageable pageable);
}
