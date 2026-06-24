package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Timing;

import java.util.Optional;

public interface TimingRepository {
    void save(Timing domain);

    Optional<Timing> findBySchoolId(String school);

    boolean existsBySchoolId(String schoolId);
}
