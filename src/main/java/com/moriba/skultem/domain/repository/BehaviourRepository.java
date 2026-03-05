package com.moriba.skultem.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Behaviour;

public interface BehaviourRepository {
    void save(Behaviour domain);

    Page<Behaviour> findAllAcademicYearandSchoolId(String academicYearId, String schoolId, Pageable pageable);
}
