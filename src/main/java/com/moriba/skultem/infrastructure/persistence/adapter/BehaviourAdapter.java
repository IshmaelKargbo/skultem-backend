package com.moriba.skultem.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.repository.BehaviourRepository;
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
    public Page<Behaviour> findAllAcademicYearandSchoolId(String academicYearId, String schoolId, Pageable pageable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
}
