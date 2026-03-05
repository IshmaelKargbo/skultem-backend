package com.moriba.skultem.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.BehaviourEntity;

@Repository
public interface BehaviourJpaRepository extends JpaRepository<BehaviourEntity, String> {
    Page<BehaviourEntity> findAllByEnrollment_AcademicYear_IdAndSchoolIdOrderByCreatedAtAsc(String academicYearId, String schoolId, Pageable pageable);
}
