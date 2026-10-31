package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.HealthCareTurnedAway;
import com.moriba.skultem.domain.repository.HealthCareTurnedAwayRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.HealthCareTurnedAwayJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.HealthCareMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HealthCareTurnedAwayAdapter implements HealthCareTurnedAwayRepository {
    private final HealthCareTurnedAwayJpaRepository repo;

    @Override
    public void save(HealthCareTurnedAway domain) {
        repo.save(HealthCareMapper.toEntity(domain));
    }

    @Override
    public List<HealthCareTurnedAway> findBetween(String schoolId, Instant from, Instant to) {
        return repo.findBySchoolIdAndTurnedAwayAtGreaterThanEqualAndTurnedAwayAtLessThanOrderByTurnedAwayAtDesc(
                schoolId, from, to).stream().map(HealthCareMapper::toDomain).toList();
    }

    @Override
    public List<HealthCareTurnedAway> findByStudent(String schoolId, String studentId) {
        return repo.findBySchoolIdAndStudentIdOrderByTurnedAwayAtDesc(schoolId, studentId).stream()
                .map(HealthCareMapper::toDomain).toList();
    }
}
