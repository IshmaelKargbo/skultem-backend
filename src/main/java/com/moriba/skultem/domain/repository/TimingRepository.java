package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.Timing;

public interface TimingRepository {
    void save(Timing domain);

    Optional<Timing> findByIdAndSchoolId(String id, String schoolId);

    List<Timing> findAllBySchoolId(String schoolId);

    Optional<Timing> findDefaultBySchoolId(String schoolId);

    boolean existsDefaultBySchoolId(String schoolId);

    void delete(Timing domain);
}
