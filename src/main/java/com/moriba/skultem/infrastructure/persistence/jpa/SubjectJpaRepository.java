package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.SubjectEntity;

@Repository
public interface SubjectJpaRepository extends JpaRepository<SubjectEntity, String> {
    boolean existsByCodeIgnoreCaseAndSchoolId(String code, String schoolId);

    Optional<SubjectEntity> findByIdAndSchoolId(String id, String schoolId);

    List<SubjectEntity> findAllByIdInAndSchoolId(Set<String> ids, String schoolId);

    Page<SubjectEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
