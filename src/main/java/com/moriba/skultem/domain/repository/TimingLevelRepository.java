package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.TimingLevel;
import com.moriba.skultem.domain.vo.Level;

public interface TimingLevelRepository {
    void save(TimingLevel domain);

    Optional<TimingLevel> findBySchoolIdAndLevel(String schoolId, Level level);

    List<TimingLevel> findAllBySchoolId(String schoolId);

    void deleteByTimingId(String timingId);

    void delete(TimingLevel domain);
}
