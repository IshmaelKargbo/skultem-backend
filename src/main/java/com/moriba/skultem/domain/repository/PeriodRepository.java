package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.model.WorkingDay;

import java.util.List;
import java.util.Optional;

public interface PeriodRepository {
    void save(Period domain);

    Optional<Period> findById(String id);

    List<Period> findAllBySessionId(String session);
}
