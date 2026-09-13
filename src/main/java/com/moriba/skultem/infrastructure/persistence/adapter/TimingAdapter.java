package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TimingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TimingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TimingAdapter implements TimingRepository {
    private final TimingJpaRepository repo;

    @Override
    public void save(Timing domain) {
        var entity = TimingMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Timing> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(TimingMapper::toDomain);
    }

    @Override
    public List<Timing> findAllBySchoolId(String schoolId) {
        return repo.findAllBySchoolId(schoolId).stream().map(TimingMapper::toDomain).toList();
    }

    @Override
    public Optional<Timing> findDefaultBySchoolId(String schoolId) {
        return repo.findBySchoolIdAndIsDefaultTrue(schoolId).map(TimingMapper::toDomain);
    }

    @Override
    public boolean existsDefaultBySchoolId(String schoolId) {
        return repo.existsBySchoolIdAndIsDefaultTrue(schoolId);
    }

    @Override
    public void delete(Timing domain) {
        repo.deleteById(domain.getId());
    }
}
