package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.vo.KindCount;
import com.moriba.skultem.infrastructure.persistence.jpa.BehaviourJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.BehaviourMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BehaviourAdapter implements BehaviourRepository {

    private final BehaviourJpaRepository repo;

    @Override
    public void save(Behaviour domain) {
        var entity = BehaviourMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<Behaviour> findAllAcademicYearAndSchoolId(String academicYearId, String schoolId, Pageable pageable) {
        return repo
                .findAllByEnrollment_AcademicYear_IdAndSchoolIdOrderByCreatedAtAsc(academicYearId, schoolId, pageable)
                .map(BehaviourMapper::toDomain);
    }

    @Override
    public Page<Behaviour> findAllAcademicYearAndClassIdAndSchoolId(String academicYearId, String classId,
            String schoolId, Pageable pageable) {
        return repo.findAllByEnrollment_AcademicYear_IdAndEnrollment_Clazz_IdAndSchoolIdOrderByCreatedAtAsc(academicYearId,
                classId, schoolId, pageable).map(BehaviourMapper::toDomain);
    }

    @Override
    public List<KindCount> countByKindForClassOrAll(String academicYearId, String schoolId, String classId) {
        return repo.countByKindForClassOrAll(academicYearId, schoolId, classId);
    }

}
